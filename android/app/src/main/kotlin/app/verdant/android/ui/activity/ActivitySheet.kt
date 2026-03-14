package app.verdant.android.ui.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.R

enum class Activity(val labelRes: Int, val icon: ImageVector) {
    ADD_SPECIES(R.string.activity_add_species, Icons.Default.Spa),
    ADD_SEEDS(R.string.activity_add_seeds, Icons.Default.Inventory),
    SOW(R.string.activity_sow, Icons.Default.Grain),
    POT_UP(R.string.activity_pot_up, Icons.Default.Inventory2),
    PLANT(R.string.activity_plant, Icons.Default.Park),
    HARVEST(R.string.activity_harvest, Icons.Default.Agriculture),
    RECOVER(R.string.activity_recover, Icons.Default.Shield),
    DISCARD(R.string.activity_discard, Icons.Default.Delete),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitySheet(
    onDismiss: () -> Unit,
    onActivitySelected: (Activity) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.what_would_you_like_to_do),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(Activity.entries.toList()) { activity ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clickable {
                                onActivitySelected(activity)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                activity.icon,
                                contentDescription = stringResource(activity.labelRes),
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(activity.labelRes),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}
