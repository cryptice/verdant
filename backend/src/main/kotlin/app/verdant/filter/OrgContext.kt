package app.verdant.filter

import jakarta.enterprise.context.RequestScoped

@RequestScoped
class OrgContext {
    var orgId: Long = 0
    var userId: Long = 0
}
