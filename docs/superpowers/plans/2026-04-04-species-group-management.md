# Species Group Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate species groups to many-to-many, add group management UI to the web app species list page, and add a species detail page showing all species info including group memberships.

**Architecture:** Replace the `species.group_id` FK with a `species_group_membership` join table. Remove system-level groups — groups are purely org-owned. Add backend endpoints for group membership CRUD. Build a collapsible group management section on the species list page and a new species detail page at `/species/:id`.

**Tech Stack:** Quarkus/Kotlin backend, PostgreSQL with Flyway, React/TypeScript with React Query, i18n.

---

## File Structure

### Backend — Modified Files
- `backend/src/main/resources/db/migration/V10__species_group_membership.sql` — new migration
- `backend/src/main/kotlin/app/verdant/entity/Species.kt` — remove groupId from Species
- `backend/src/main/kotlin/app/verdant/dto/SpeciesDtos.kt` — replace groupId/groupName with groups list
- `backend/src/main/kotlin/app/verdant/repository/SpeciesRepository.kt` — remove groupId from SQL, update findByGroupId
- `backend/src/main/kotlin/app/verdant/repository/SpeciesGroupRepository.kt` — add membership CRUD
- `backend/src/main/kotlin/app/verdant/service/SpeciesService.kt` — many-to-many mapping, remove admin group methods
- `backend/src/main/kotlin/app/verdant/resource/SpeciesResource.kt` — add membership endpoints, add rename
- `backend/src/main/kotlin/app/verdant/resource/AdminResource.kt` — remove group endpoints
- `backend/src/main/kotlin/app/verdant/service/DataExportService.kt` — update species export
- `backend/src/main/kotlin/app/verdant/service/ScheduledTaskService.kt` — no change (uses findByGroupId which is updated)
- `backend/src/main/kotlin/app/verdant/resource/DevResource.kt` — update seeding

### Frontend — New Files
- `web/src/pages/SpeciesDetail.tsx` — species detail page
- `web/src/components/GroupManagement.tsx` — collapsible group management section

### Frontend — Modified Files
- `web/src/api/client.ts` — updated types and new API methods
- `web/src/pages/SpeciesList.tsx` — integrate GroupManagement, link to detail page
- `web/src/App.tsx` — add species detail route
- `web/src/i18n/en.json` and `sv.json` — new translation keys
- `admin/src/pages/Species.tsx` — remove ManageGroupsSection and group dropdown

---

### Task 1: Database Migration — Many-to-Many Join Table

**Files:**
- Create: `backend/src/main/resources/db/migration/V10__species_group_membership.sql`

- [ ] **Step 1: Write the migration**

```sql
-- Many-to-many join table for species <-> groups
CREATE TABLE species_group_membership (
    species_id  BIGINT NOT NULL REFERENCES species(id) ON DELETE CASCADE,
    group_id    BIGINT NOT NULL REFERENCES species_group(id) ON DELETE CASCADE,
    PRIMARY KEY (species_id, group_id)
);

CREATE INDEX idx_species_group_membership_species ON species_group_membership(species_id);
CREATE INDEX idx_species_group_membership_group ON species_group_membership(group_id);

-- Backfill from existing group_id column
INSERT INTO species_group_membership (species_id, group_id)
SELECT id, group_id FROM species WHERE group_id IS NOT NULL;

-- Drop the old column
ALTER TABLE species DROP COLUMN group_id;

-- Remove system-level groups (org_id IS NULL) — groups are now org-only
-- First remove their memberships (CASCADE handles this via the FK on species_group_membership)
DELETE FROM species_group WHERE org_id IS NULL;
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/resources/db/migration/V10__species_group_membership.sql
git commit -m "feat: migrate species groups to many-to-many, remove system groups"
```

---

### Task 2: Backend Entity — Remove groupId from Species

**Files:**
- Modify: `backend/src/main/kotlin/app/verdant/entity/Species.kt`

- [ ] **Step 1: Remove groupId field from Species data class**

Remove the line `val groupId: Long? = null,` from the Species data class. The field is around line 25.

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/entity/Species.kt
git commit -m "feat: remove groupId from Species entity"
```

---

### Task 3: Backend DTOs — Replace groupId/groupName with Groups List

**Files:**
- Modify: `backend/src/main/kotlin/app/verdant/dto/SpeciesDtos.kt`

- [ ] **Step 1: Update SpeciesResponse**

Replace the `groupId` and `groupName` fields (lines 38-39) with:

```kotlin
    val groups: List<SpeciesGroupResponse>,
```

- [ ] **Step 2: Remove groupId from CreateSpeciesRequest and UpdateSpeciesRequest**

Remove `val groupId: Long? = null,` from both request DTOs (line 84 in Create, line 132 in Update).

- [ ] **Step 3: Update SpeciesExportEntry**

Replace `val groupName: String? = null,` (line 278) with:

```kotlin
    val groupNames: List<String> = emptyList(),
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/dto/SpeciesDtos.kt
git commit -m "feat: update DTOs for many-to-many species groups"
```

---

### Task 4: Backend Repository — SpeciesGroupRepository Membership CRUD

**Files:**
- Modify: `backend/src/main/kotlin/app/verdant/repository/SpeciesGroupRepository.kt`

- [ ] **Step 1: Add membership methods**

Add these methods to `SpeciesGroupRepository`:

```kotlin
    fun findGroupIdsBySpeciesId(speciesId: Long): List<Long> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT group_id FROM species_group_membership WHERE species_id = ?").use { ps ->
                ps.setLong(1, speciesId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.getLong("group_id")) }
                }
            }
        }

    fun findGroupIdsBySpeciesIds(speciesIds: Set<Long>): Map<Long, List<Long>> {
        if (speciesIds.isEmpty()) return emptyMap()
        val placeholders = speciesIds.joinToString(",") { "?" }
        return ds.connection.use { conn ->
            conn.prepareStatement("SELECT species_id, group_id FROM species_group_membership WHERE species_id IN ($placeholders)").use { ps ->
                speciesIds.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                ps.executeQuery().use { rs ->
                    val result = mutableMapOf<Long, MutableList<Long>>()
                    while (rs.next()) {
                        result.getOrPut(rs.getLong("species_id")) { mutableListOf() }
                            .add(rs.getLong("group_id"))
                    }
                    result
                }
            }
        }
    }

    fun findSpeciesIdsByGroupId(groupId: Long): List<Long> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT species_id FROM species_group_membership WHERE group_id = ?").use { ps ->
                ps.setLong(1, groupId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.getLong("species_id")) }
                }
            }
        }

    fun addSpeciesToGroup(speciesId: Long, groupId: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO species_group_membership (species_id, group_id) VALUES (?, ?) ON CONFLICT DO NOTHING"
            ).use { ps ->
                ps.setLong(1, speciesId)
                ps.setLong(2, groupId)
                ps.executeUpdate()
            }
        }
    }

    fun removeSpeciesFromGroup(speciesId: Long, groupId: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM species_group_membership WHERE species_id = ? AND group_id = ?").use { ps ->
                ps.setLong(1, speciesId)
                ps.setLong(2, groupId)
                ps.executeUpdate()
            }
        }
    }
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/repository/SpeciesGroupRepository.kt
git commit -m "feat: add group membership CRUD to SpeciesGroupRepository"
```

---

### Task 5: Backend Repository — Remove groupId from SpeciesRepository

**Files:**
- Modify: `backend/src/main/kotlin/app/verdant/repository/SpeciesRepository.kt`

- [ ] **Step 1: Remove groupId from persist SQL and bind**

In the `persist` method, remove `group_id` from the INSERT column list and remove the `ps.setObject(19, species.groupId)` bind. Renumber subsequent binds.

- [ ] **Step 2: Remove groupId from update SQL and bind**

In the `update` method, remove `group_id = ?` from the UPDATE SET clause and remove the corresponding bind. Renumber subsequent binds.

- [ ] **Step 3: Remove groupId from toSpecies() mapper**

Remove `groupId = getObject("group_id") as? Long,` from the `ResultSet.toSpecies()` extension function.

- [ ] **Step 4: Update findByGroupId to use the join table**

Replace the existing `findByGroupId` method:

```kotlin
    fun findByGroupId(groupId: Long): List<Species> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT s.* FROM species s
                   JOIN species_group_membership m ON s.id = m.species_id
                   WHERE m.group_id = ?
                   ORDER BY s.common_name"""
            ).use { ps ->
                ps.setLong(1, groupId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSpecies()) }
                }
            }
        }
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/repository/SpeciesRepository.kt
git commit -m "feat: remove groupId from SpeciesRepository, use join table for findByGroupId"
```

---

### Task 6: Backend Service — Update SpeciesService for Many-to-Many

**Files:**
- Modify: `backend/src/main/kotlin/app/verdant/service/SpeciesService.kt`

This is a large file with many changes. Read it first, then make these changes:

- [ ] **Step 1: Update mapSpeciesList to use group memberships**

The `mapSpeciesList` method currently takes `groups: Map<Long?, SpeciesGroup>`. Update it to also fetch group memberships and pass them through. Replace the existing groups lookup pattern.

In `getSpeciesForUser`, `getSpeciesByGroup`, `searchSpeciesForUser`, and all admin list methods, instead of:
```kotlin
val groups = groupRepository.findByOrgId(orgId).associateBy { it.id }
```

Keep that, but also fetch memberships inside `mapSpeciesList`:
```kotlin
val groupIdsBySpecies = groupRepository.findGroupIdsBySpeciesIds(ids)
```

- [ ] **Step 2: Update toResponse to use groups list**

Replace the single `groupId`/`groupName` mapping with a list:

```kotlin
groups = (groupIdsBySpecies[species.id] ?: emptyList()).mapNotNull { gid ->
    groups[gid]?.let { SpeciesGroupResponse(it.id!!, it.name) }
},
```

Remove all references to `species.groupId` and `groupName` in the toResponse methods.

- [ ] **Step 3: Update getSpecies (single species) to use groups list**

The `getSpecies` method currently does:
```kotlin
val groups: Map<Long?, SpeciesGroup> = species.groupId?.let { ... }
```

Replace with:
```kotlin
val groupIds = groupRepository.findGroupIdsBySpeciesId(species.id!!)
val groups = if (groupIds.isNotEmpty()) {
    groupIds.mapNotNull { groupRepository.findById(it) }.associateBy { it.id }
} else emptyMap()
```

Similarly update `getSpeciesAdmin`.

- [ ] **Step 4: Remove groupId from createSpecies and updateSpecies**

Remove `groupId = request.groupId` from the Species constructor in `createSpecies` and the `.copy()` in `updateSpecies`. Also remove it from the admin variants.

- [ ] **Step 5: Remove admin group methods**

Remove these methods:
- `getAllGroups()`
- `createGroupAdmin(request)`
- `updateGroupAdmin(id, request)`
- `deleteGroupAdmin(id)`

- [ ] **Step 6: Add updateGroup method for user-level rename**

```kotlin
    fun updateGroup(groupId: Long, request: CreateSpeciesGroupRequest, orgId: Long): SpeciesGroupResponse {
        val group = groupRepository.findById(groupId) ?: throw NotFoundException("Group not found")
        if (group.orgId != orgId) throw NotFoundException("Group not found")
        val updated = group.copy(name = request.name)
        groupRepository.update(updated)
        return SpeciesGroupResponse(updated.id!!, updated.name)
    }
```

- [ ] **Step 7: Add group membership service methods**

```kotlin
    fun addSpeciesToGroup(groupId: Long, speciesId: Long, orgId: Long) {
        val group = groupRepository.findById(groupId) ?: throw NotFoundException("Group not found")
        if (group.orgId != orgId) throw NotFoundException("Group not found")
        speciesRepository.findById(speciesId) ?: throw NotFoundException("Species not found")
        groupRepository.addSpeciesToGroup(speciesId, groupId)
    }

    fun removeSpeciesFromGroup(groupId: Long, speciesId: Long, orgId: Long) {
        val group = groupRepository.findById(groupId) ?: throw NotFoundException("Group not found")
        if (group.orgId != orgId) throw NotFoundException("Group not found")
        groupRepository.removeSpeciesFromGroup(speciesId, groupId)
    }

    fun getGroupMembers(groupId: Long, orgId: Long): List<SpeciesResponse> {
        val group = groupRepository.findById(groupId) ?: throw NotFoundException("Group not found")
        if (group.orgId != orgId) throw NotFoundException("Group not found")
        val allGroups = groupRepository.findByOrgId(orgId).associateBy { it.id }
        val tags = tagRepository.findByOrgId(orgId).associateBy { it.id }
        val speciesList = speciesRepository.findByGroupId(groupId)
        return mapSpeciesList(speciesList, allGroups, tags)
    }
```

- [ ] **Step 8: Update export/import methods**

In `exportSpecies`, replace:
```kotlin
groupName = species.groupId?.let { groups[it]?.name },
```
with:
```kotlin
groupNames = (groupIdsBySpecies[species.id] ?: emptyList()).mapNotNull { groups[it]?.name },
```

In `importSpecies`, update the group handling to use the join table instead of `groupId` on species.

- [ ] **Step 9: Verify compilation**

Run: `cd backend && ./gradlew compileKotlin`

- [ ] **Step 10: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/service/SpeciesService.kt
git commit -m "feat: update SpeciesService for many-to-many groups"
```

---

### Task 7: Backend Resource — Group Membership Endpoints

**Files:**
- Modify: `backend/src/main/kotlin/app/verdant/resource/SpeciesResource.kt`

- [ ] **Step 1: Add rename group endpoint and membership endpoints**

Add these endpoints to SpeciesResource:

```kotlin
    @PUT
    @Path("/groups/{id}")
    fun updateGroup(@PathParam("id") id: Long, @Valid request: CreateSpeciesGroupRequest): Response {
        val group = speciesService.updateGroup(id, request, orgContext.orgId)
        return Response.ok(group).build()
    }

    @GET
    @Path("/groups/{groupId}/species")
    fun listGroupMembers(@PathParam("groupId") groupId: Long) =
        speciesService.getGroupMembers(groupId, orgContext.orgId)

    @POST
    @Path("/groups/{groupId}/species/{speciesId}")
    fun addSpeciesToGroup(@PathParam("groupId") groupId: Long, @PathParam("speciesId") speciesId: Long): Response {
        speciesService.addSpeciesToGroup(groupId, speciesId, orgContext.orgId)
        return Response.noContent().build()
    }

    @DELETE
    @Path("/groups/{groupId}/species/{speciesId}")
    fun removeSpeciesFromGroup(@PathParam("groupId") groupId: Long, @PathParam("speciesId") speciesId: Long): Response {
        speciesService.removeSpeciesFromGroup(groupId, speciesId, orgContext.orgId)
        return Response.noContent().build()
    }
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/resource/SpeciesResource.kt
git commit -m "feat: add group membership and rename endpoints"
```

---

### Task 8: Backend — Remove Admin Group Endpoints

**Files:**
- Modify: `backend/src/main/kotlin/app/verdant/resource/AdminResource.kt`

- [ ] **Step 1: Remove the admin group endpoints**

Remove the four group endpoints (listGroups, createGroup, updateGroup, deleteGroup) from AdminResource. These are in the `// ── Groups ──` section.

- [ ] **Step 2: Update DevResource seeding**

Read `backend/src/main/kotlin/app/verdant/resource/DevResource.kt` and update:
- Group creation should use `orgId` instead of null
- Group assignment should use `groupRepository.addSpeciesToGroup(speciesId, groupId)` instead of setting `groupId` on Species
- Remove `groupId = ...` from all Species constructor calls in the seed method

- [ ] **Step 3: Update admin UI Species.tsx**

Read `admin/src/pages/Species.tsx`. Remove:
- The `ManageGroupsSection` component
- The group dropdown from the species edit form
- Related state, queries, and mutations for groups

- [ ] **Step 4: Update admin API client**

Remove group-related methods from `admin/src/api/client.ts`:
- `getSpeciesGroups`
- `createSpeciesGroup`
- `updateSpeciesGroup`
- `deleteSpeciesGroup`

Remove `groupId` and `groupName` from the admin `Species` interface, replace with `groups: { id: number; name: string }[]`.

- [ ] **Step 5: Verify compilation**

Run: `cd backend && ./gradlew compileKotlin`

- [ ] **Step 6: Commit**

```bash
git add backend/ admin/
git commit -m "feat: remove admin group management, update seeding for many-to-many"
```

---

### Task 9: Backend Tests

**Files:**
- Modify: `backend/src/test/kotlin/app/verdant/service/ScheduledTaskServiceTest.kt`

- [ ] **Step 1: Verify existing tests still pass**

The ScheduledTaskServiceTest uses `makeSpecies(id, name, groupId)` — since groupId was removed from Species entity, update the helper. Remove the `groupId` parameter from `makeSpecies`.

Run: `cd backend && ./gradlew test`

- [ ] **Step 2: Commit if changes were needed**

```bash
git add backend/src/test/
git commit -m "fix: update tests for Species entity without groupId"
```

---

### Task 10: Frontend API Client — Updated Types and New Methods

**Files:**
- Modify: `web/src/api/client.ts`

- [ ] **Step 1: Update SpeciesResponse type**

Replace the current `SpeciesResponse` interface. The key changes:
- Remove any `groupId`/`groupName` fields (if present — check the current TS type)
- Add `groups` field
- Add `isSystem` field

The updated type should include:
```typescript
  groups: { id: number; name: string }[]
  isSystem: boolean
```

- [ ] **Step 2: Add new API methods**

Add to the `speciesGroups` section:

```typescript
  speciesGroups: {
    list: () => apiRequest<{ id: number; name: string }[]>('/api/species/groups'),
    create: (name: string) =>
      apiRequest<{ id: number; name: string }>('/api/species/groups', { method: 'POST', body: JSON.stringify({ name }) }),
    update: (id: number, name: string) =>
      apiRequest<{ id: number; name: string }>(`/api/species/groups/${id}`, { method: 'PUT', body: JSON.stringify({ name }) }),
    delete: (id: number) => apiRequest<void>(`/api/species/groups/${id}`, { method: 'DELETE' }),
    members: (groupId: number) => apiRequest<SpeciesResponse[]>(`/api/species/groups/${groupId}/species`),
    addSpecies: (groupId: number, speciesId: number) =>
      apiRequest<void>(`/api/species/groups/${groupId}/species/${speciesId}`, { method: 'POST' }),
    removeSpecies: (groupId: number, speciesId: number) =>
      apiRequest<void>(`/api/species/groups/${groupId}/species/${speciesId}`, { method: 'DELETE' }),
  },
```

- [ ] **Step 3: Add species.get method if not present**

Check if `api.species.get(id)` exists. If not, add:
```typescript
    get: (id: number) => apiRequest<SpeciesResponse>(`/api/species/${id}`),
```

- [ ] **Step 4: Commit**

```bash
git add web/src/api/client.ts
git commit -m "feat: update frontend API types for many-to-many groups"
```

---

### Task 11: Frontend — GroupManagement Component

**Files:**
- Create: `web/src/components/GroupManagement.tsx`

- [ ] **Step 1: Create the component**

```tsx
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'
import { SpeciesAutocomplete } from './SpeciesAutocomplete'

function speciesLabel(s: SpeciesResponse, lang: string) {
  const name = lang === 'sv' ? (s.commonNameSv ?? s.commonName) : s.commonName
  const variant = lang === 'sv' ? (s.variantNameSv ?? s.variantName) : s.variantName
  return variant ? `${name} — ${variant}` : name
}

export function GroupManagement() {
  const { t, i18n } = useTranslation()
  const qc = useQueryClient()
  const [open, setOpen] = useState(false)
  const [newName, setNewName] = useState('')
  const [expandedGroupId, setExpandedGroupId] = useState<number | null>(null)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editingName, setEditingName] = useState('')

  const { data: groups } = useQuery({
    queryKey: ['species-groups'],
    queryFn: api.speciesGroups.list,
    enabled: open,
  })

  const { data: members } = useQuery({
    queryKey: ['group-members', expandedGroupId],
    queryFn: () => api.speciesGroups.members(expandedGroupId!),
    enabled: !!expandedGroupId,
  })

  const createMut = useMutation({
    mutationFn: (name: string) => api.speciesGroups.create(name),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['species-groups'] }); setNewName('') },
  })

  const renameMut = useMutation({
    mutationFn: ({ id, name }: { id: number; name: string }) => api.speciesGroups.update(id, name),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['species-groups'] }); setEditingId(null) },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.speciesGroups.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['species-groups'] })
      qc.invalidateQueries({ queryKey: ['species'] })
      if (expandedGroupId === deleteMut.variables) setExpandedGroupId(null)
    },
  })

  const addSpeciesMut = useMutation({
    mutationFn: ({ groupId, speciesId }: { groupId: number; speciesId: number }) =>
      api.speciesGroups.addSpecies(groupId, speciesId),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['group-members', vars.groupId] })
      qc.invalidateQueries({ queryKey: ['species'] })
    },
  })

  const removeSpeciesMut = useMutation({
    mutationFn: ({ groupId, speciesId }: { groupId: number; speciesId: number }) =>
      api.speciesGroups.removeSpecies(groupId, speciesId),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['group-members', vars.groupId] })
      qc.invalidateQueries({ queryKey: ['species'] })
    },
  })

  return (
    <div className="mx-4 mb-4">
      <button
        onClick={() => setOpen(!open)}
        className="w-full flex items-center justify-between px-4 py-3 bg-surface rounded-xl border border-divider text-sm font-medium"
      >
        <span>{t('groups.manageGroups')}</span>
        <span className="text-text-secondary">{open ? '▲' : '▼'}</span>
      </button>

      {open && (
        <div className="mt-2 border border-divider rounded-xl bg-bg p-4 space-y-3">
          {/* Create new group */}
          <div className="flex gap-2">
            <input
              value={newName}
              onChange={e => setNewName(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter' && newName.trim()) createMut.mutate(newName.trim()) }}
              placeholder={t('groups.newGroupPlaceholder')}
              className="input flex-1"
            />
            <button
              onClick={() => newName.trim() && createMut.mutate(newName.trim())}
              disabled={!newName.trim() || createMut.isPending}
              className="btn-primary text-sm px-4"
            >
              {t('groups.create')}
            </button>
          </div>

          {/* Group list */}
          {groups?.map(group => (
            <div key={group.id} className="border border-divider rounded-xl overflow-hidden">
              {/* Group header */}
              <div className="flex items-center gap-2 px-3 py-2 bg-surface">
                {editingId === group.id ? (
                  <input
                    autoFocus
                    value={editingName}
                    onChange={e => setEditingName(e.target.value)}
                    onKeyDown={e => {
                      if (e.key === 'Enter' && editingName.trim()) renameMut.mutate({ id: group.id, name: editingName.trim() })
                      if (e.key === 'Escape') setEditingId(null)
                    }}
                    onBlur={() => setEditingId(null)}
                    className="input text-sm flex-1"
                  />
                ) : (
                  <>
                    <button
                      onClick={() => setExpandedGroupId(expandedGroupId === group.id ? null : group.id)}
                      className="flex-1 text-left text-sm font-medium"
                    >
                      <span className="mr-1 text-text-secondary">{expandedGroupId === group.id ? '▾' : '▸'}</span>
                      {group.name}
                    </button>
                    <button
                      onClick={() => { setEditingId(group.id); setEditingName(group.name) }}
                      className="text-xs text-text-secondary hover:text-text px-1"
                    >
                      {t('common.edit')}
                    </button>
                    <button
                      onClick={() => deleteMut.mutate(group.id)}
                      className="text-xs text-error px-1"
                    >
                      {t('common.delete')}
                    </button>
                  </>
                )}
              </div>

              {/* Expanded: members + add */}
              {expandedGroupId === group.id && (
                <div className="px-3 py-2 space-y-2">
                  {members?.map(s => (
                    <div key={s.id} className="flex items-center justify-between text-sm py-1">
                      <span>{speciesLabel(s, i18n.language)}</span>
                      <button
                        onClick={() => removeSpeciesMut.mutate({ groupId: group.id, speciesId: s.id })}
                        className="text-xs text-error"
                      >
                        {t('groups.remove')}
                      </button>
                    </div>
                  ))}
                  {members?.length === 0 && (
                    <p className="text-xs text-text-secondary">{t('groups.empty')}</p>
                  )}
                  <div className="pt-1">
                    <SpeciesAutocomplete
                      value={null}
                      onChange={s => {
                        if (s) addSpeciesMut.mutate({ groupId: group.id, speciesId: s.id })
                      }}
                      placeholder={t('groups.addSpeciesPlaceholder')}
                    />
                  </div>
                </div>
              )}
            </div>
          ))}

          {groups?.length === 0 && (
            <p className="text-xs text-text-secondary text-center py-2">{t('groups.noGroups')}</p>
          )}
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add web/src/components/GroupManagement.tsx
git commit -m "feat: GroupManagement component for species list page"
```

---

### Task 12: Frontend — Species Detail Page

**Files:**
- Create: `web/src/pages/SpeciesDetail.tsx`
- Modify: `web/src/App.tsx`

- [ ] **Step 1: Create the species detail page**

```tsx
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useState } from 'react'
import { api, type SpeciesResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import type { BreadcrumbItem } from '../components/Breadcrumb'

function Field({ label, value }: { label: string; value?: string | number | null }) {
  if (value == null || value === '') return null
  return (
    <div>
      <dt className="text-xs text-text-secondary">{label}</dt>
      <dd className="text-sm mt-0.5">{value}</dd>
    </div>
  )
}

function MonthList({ label, months }: { label: string; months?: number[] }) {
  if (!months || months.length === 0) return null
  const names = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
  return (
    <div>
      <dt className="text-xs text-text-secondary">{label}</dt>
      <dd className="text-sm mt-0.5">{months.map(m => names[m - 1]).join(', ')}</dd>
    </div>
  )
}

export function SpeciesDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t, i18n } = useTranslation()
  const [showDelete, setShowDelete] = useState(false)

  const { data: species, error, isLoading, refetch } = useQuery({
    queryKey: ['species', Number(id)],
    queryFn: () => api.species.get(Number(id)),
    enabled: !!id,
  })

  const deleteMut = useMutation({
    mutationFn: () => api.species.delete(Number(id)),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['species'] }); navigate('/species', { replace: true }) },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />
  if (!species) return null

  const lang = i18n.language
  const name = lang === 'sv' ? (species.commonNameSv ?? species.commonName) : species.commonName
  const variant = lang === 'sv' ? (species.variantNameSv ?? species.variantName) : species.variantName
  const displayName = variant ? `${name} — ${variant}` : name
  const canEdit = !species.isSystem

  const breadcrumbs: BreadcrumbItem[] = [{ label: t('species.title'), to: '/species' }]

  return (
    <div className="max-w-lg">
      <PageHeader title={displayName} breadcrumbs={breadcrumbs} />

      <div className="px-4 space-y-4">
        {/* Basic Info */}
        <section className="form-card">
          <h2 className="text-xs font-semibold text-text-secondary uppercase tracking-wide mb-2">{t('speciesDetail.basicInfo')}</h2>
          <dl className="grid grid-cols-2 gap-3">
            <Field label={t('species.commonName')} value={species.commonName} />
            <Field label={t('species.variantName')} value={species.variantName} />
            <Field label={t('speciesDetail.commonNameSv')} value={species.commonNameSv} />
            <Field label={t('species.variantNameSv')} value={species.variantNameSv} />
            <Field label={t('species.scientificName')} value={species.scientificName} />
            <Field label={t('speciesDetail.plantType')} value={species.plantType ? t(`plantTypes.${species.plantType}`, { defaultValue: species.plantType }) : undefined} />
            <Field label={t('speciesDetail.unitType')} value={species.defaultUnitType ? t(`unitTypes.${species.defaultUnitType}`, { defaultValue: species.defaultUnitType }) : undefined} />
          </dl>
        </section>

        {/* Growing Info */}
        {(species.daysToSprout || species.daysToHarvest || species.germinationTimeDays || species.sowingDepthMm || species.heightCm || species.germinationRate) && (
          <section className="form-card">
            <h2 className="text-xs font-semibold text-text-secondary uppercase tracking-wide mb-2">{t('speciesDetail.growing')}</h2>
            <dl className="grid grid-cols-2 gap-3">
              <Field label={t('speciesDetail.daysToSprout')} value={species.daysToSprout} />
              <Field label={t('speciesDetail.daysToHarvest')} value={species.daysToHarvest} />
              <Field label={t('speciesDetail.germinationTime')} value={species.germinationTimeDays ? `${species.germinationTimeDays} ${t('speciesDetail.days')}` : undefined} />
              <Field label={t('speciesDetail.sowingDepth')} value={species.sowingDepthMm ? `${species.sowingDepthMm} mm` : undefined} />
              <Field label={t('speciesDetail.height')} value={species.heightCm ? `${species.heightCm} cm` : undefined} />
              <Field label={t('speciesDetail.germinationRate')} value={species.germinationRate ? `${species.germinationRate}%` : undefined} />
              <MonthList label={t('speciesDetail.bloomMonths')} months={species.bloomMonths} />
              <MonthList label={t('speciesDetail.sowingMonths')} months={species.sowingMonths} />
            </dl>
          </section>
        )}

        {/* Commercial */}
        {(species.costPerSeedSek || species.expectedStemsPerPlant || species.expectedVaseLifeDays) && (
          <section className="form-card">
            <h2 className="text-xs font-semibold text-text-secondary uppercase tracking-wide mb-2">{t('speciesDetail.commercial')}</h2>
            <dl className="grid grid-cols-2 gap-3">
              <Field label={t('speciesDetail.costPerSeed')} value={species.costPerSeedSek ? `${species.costPerSeedSek} kr` : undefined} />
              <Field label={t('speciesDetail.stemsPerPlant')} value={species.expectedStemsPerPlant} />
              <Field label={t('speciesDetail.vaseLife')} value={species.expectedVaseLifeDays ? `${species.expectedVaseLifeDays} ${t('speciesDetail.days')}` : undefined} />
            </dl>
          </section>
        )}

        {/* Groups */}
        {species.groups.length > 0 && (
          <section className="form-card">
            <h2 className="text-xs font-semibold text-text-secondary uppercase tracking-wide mb-2">{t('speciesDetail.groups')}</h2>
            <div className="flex flex-wrap gap-2">
              {species.groups.map(g => (
                <span key={g.id} className="text-xs bg-accent/15 text-accent px-2 py-1 rounded-lg">{g.name}</span>
              ))}
            </div>
          </section>
        )}

        {/* Tags */}
        {species.tags.length > 0 && (
          <section className="form-card">
            <h2 className="text-xs font-semibold text-text-secondary uppercase tracking-wide mb-2">{t('speciesDetail.tags')}</h2>
            <div className="flex flex-wrap gap-2">
              {species.tags.map(tag => (
                <span key={tag.id} className="text-xs bg-surface border border-divider px-2 py-1 rounded-lg">{tag.name}</span>
              ))}
            </div>
          </section>
        )}

        {/* Providers */}
        {species.providers.length > 0 && (
          <section className="form-card">
            <h2 className="text-xs font-semibold text-text-secondary uppercase tracking-wide mb-2">{t('speciesDetail.providers')}</h2>
            <div className="space-y-2">
              {species.providers.map(p => (
                <div key={p.id} className="text-sm">
                  <span className="font-medium">{p.providerName}</span>
                  {p.costPerUnitSek != null && <span className="text-text-secondary ml-2">{p.costPerUnitSek} kr/{p.unitType?.toLowerCase()}</span>}
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Actions */}
        {canEdit && (
          <div className="flex gap-2 pb-8">
            <button onClick={() => setShowDelete(true)} className="text-sm text-error">{t('common.delete')}</button>
          </div>
        )}
      </div>

      {showDelete && (
        <Dialog open={showDelete} title={t('species.deleteSpeciesTitle')} onClose={() => setShowDelete(false)}>
          <p className="text-sm mb-4">{t('common.delete')} &ldquo;{displayName}&rdquo;?</p>
          <div className="flex gap-2">
            <button className="btn-secondary flex-1" onClick={() => setShowDelete(false)}>{t('common.cancel')}</button>
            <button
              className="btn-primary flex-1 bg-error"
              onClick={() => deleteMut.mutate()}
              disabled={deleteMut.isPending}
            >
              {deleteMut.isPending ? t('common.deleting') : t('common.delete')}
            </button>
          </div>
        </Dialog>
      )}
    </div>
  )
}
```

- [ ] **Step 2: Add route to App.tsx**

Add import and route:
```tsx
import { SpeciesDetail } from './pages/SpeciesDetail'
```

Add route after the existing species route (line 81):
```tsx
        <Route path="species/:id" element={<SpeciesDetail />} />
```

- [ ] **Step 3: Update SpeciesList to navigate to detail page**

In `web/src/pages/SpeciesList.tsx`, change the table row `onClick` (line 118) from:
```tsx
onClick={() => navigate(`/sow?speciesId=${s.id}`)}
```
to:
```tsx
onClick={() => navigate(`/species/${s.id}`)}
```

- [ ] **Step 4: Commit**

```bash
git add web/src/pages/SpeciesDetail.tsx web/src/App.tsx web/src/pages/SpeciesList.tsx
git commit -m "feat: species detail page with all info and groups"
```

---

### Task 13: Frontend — Integrate GroupManagement into SpeciesList

**Files:**
- Modify: `web/src/pages/SpeciesList.tsx`

- [ ] **Step 1: Add GroupManagement component**

Import and add the component above the search bar:

```tsx
import { GroupManagement } from '../components/GroupManagement'
```

Add `<GroupManagement />` between `<OnboardingHint />` and the search input `<div>`.

- [ ] **Step 2: Commit**

```bash
git add web/src/pages/SpeciesList.tsx
git commit -m "feat: integrate group management into species list page"
```

---

### Task 14: Frontend — i18n Translation Keys

**Files:**
- Modify: `web/src/i18n/en.json` and `web/src/i18n/sv.json`

- [ ] **Step 1: Add all new keys**

English additions:
```json
"groups.manageGroups": "Manage Groups",
"groups.newGroupPlaceholder": "New group name...",
"groups.create": "Create",
"groups.remove": "Remove",
"groups.empty": "No species in this group",
"groups.noGroups": "No groups yet",
"groups.addSpeciesPlaceholder": "Search to add species...",
"speciesDetail.basicInfo": "Basic Info",
"speciesDetail.commonNameSv": "Swedish Name",
"speciesDetail.plantType": "Plant Type",
"speciesDetail.unitType": "Unit Type",
"speciesDetail.growing": "Growing",
"speciesDetail.daysToSprout": "Days to Sprout",
"speciesDetail.daysToHarvest": "Days to Harvest",
"speciesDetail.germinationTime": "Germination Time",
"speciesDetail.sowingDepth": "Sowing Depth",
"speciesDetail.height": "Height",
"speciesDetail.germinationRate": "Germination Rate",
"speciesDetail.bloomMonths": "Bloom Months",
"speciesDetail.sowingMonths": "Sowing Months",
"speciesDetail.days": "days",
"speciesDetail.commercial": "Commercial",
"speciesDetail.costPerSeed": "Cost per Seed",
"speciesDetail.stemsPerPlant": "Stems per Plant",
"speciesDetail.vaseLife": "Vase Life",
"speciesDetail.groups": "Groups",
"speciesDetail.tags": "Tags",
"speciesDetail.providers": "Providers"
```

Swedish additions:
```json
"groups.manageGroups": "Hantera grupper",
"groups.newGroupPlaceholder": "Nytt gruppnamn...",
"groups.create": "Skapa",
"groups.remove": "Ta bort",
"groups.empty": "Inga arter i denna grupp",
"groups.noGroups": "Inga grupper ännu",
"groups.addSpeciesPlaceholder": "Sök för att lägga till art...",
"speciesDetail.basicInfo": "Grundinfo",
"speciesDetail.commonNameSv": "Svenskt namn",
"speciesDetail.plantType": "Växttyp",
"speciesDetail.unitType": "Enhetstyp",
"speciesDetail.growing": "Odling",
"speciesDetail.daysToSprout": "Dagar till grodd",
"speciesDetail.daysToHarvest": "Dagar till skörd",
"speciesDetail.germinationTime": "Grodningstid",
"speciesDetail.sowingDepth": "Sådjup",
"speciesDetail.height": "Höjd",
"speciesDetail.germinationRate": "Groningsgrad",
"speciesDetail.bloomMonths": "Blomningsmånader",
"speciesDetail.sowingMonths": "Såningsmånader",
"speciesDetail.days": "dagar",
"speciesDetail.commercial": "Kommersiellt",
"speciesDetail.costPerSeed": "Kostnad per frö",
"speciesDetail.stemsPerPlant": "Stjälkar per planta",
"speciesDetail.vaseLife": "Vaslivslängd",
"speciesDetail.groups": "Grupper",
"speciesDetail.tags": "Taggar",
"speciesDetail.providers": "Leverantörer"
```

- [ ] **Step 2: Commit**

```bash
git add web/src/i18n/
git commit -m "feat: add i18n keys for group management and species detail"
```

---

### Task 15: End-to-End Verification

- [ ] **Step 1: Run backend tests**

Run: `cd backend && ./gradlew test`
Expected: All tests pass.

- [ ] **Step 2: Run backend compilation**

Run: `cd backend && ./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run frontend type check**

Run: `cd web && npx tsc --noEmit`
Expected: No type errors.

- [ ] **Step 4: Run admin UI type check**

Run: `cd admin && npx tsc --noEmit`
Expected: No type errors.

- [ ] **Step 5: Manual verification checklist**

1. Species list page shows "Manage Groups" collapsible section
2. Can create a new group
3. Can expand a group, see members
4. Can search and add a species to a group
5. Can remove a species from a group
6. Can rename a group
7. Can delete a group
8. Clicking a species in the list navigates to detail page
9. Species detail page shows all fields organized in sections
10. Species detail page shows groups the species belongs to
11. System species show no edit/delete buttons
12. Org species show delete button
13. Task creation with group still works (regression check)
