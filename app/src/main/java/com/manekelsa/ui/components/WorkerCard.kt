package com.manekelsa.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.manekelsa.R
import com.manekelsa.data.local.entity.WorkerEntity
import com.manekelsa.ui.components.SkillChipLabel
import com.manekelsa.utils.TranslationUtils
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerCard(
    worker: WorkerEntity,
    hasRatedToday: Boolean,
    onClick: () -> Unit,
    onRateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val saffronColor = Color(0xFFFF9933)
    val context = LocalContext.current
    var showCallDialog by remember { mutableStateOf(false) }
    var showRateSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    if (showCallDialog) {
        AlertDialog(
            onDismissRequest = { showCallDialog = false },
            title = { Text(stringResource(R.string.worker_card_call_title, worker.name)) },
            text = { Text(stringResource(R.string.worker_card_call_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCallDialog = false
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${worker.phoneNumber}")
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text(stringResource(R.string.worker_card_call), color = saffronColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCallDialog = false }) {
                    Text(stringResource(R.string.worker_card_cancel), color = Color.Gray)
                }
            }
        )
    }

    if (showRateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRateSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.worker_card_rate_prompt),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Button(
                    onClick = {
                        onRateClick()
                        showRateSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = saffronColor)
                ) {
                    Icon(Icons.Default.ThumbUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.worker_card_thumbs_up), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { showRateSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(stringResource(R.string.worker_card_not_now), fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Photo
                Box(modifier = Modifier.size(80.dp)) {
                    AsyncImage(
                        model = worker.photoUrl ?: "https://ui-avatars.com/api/?name=${worker.name.replace(" ", "+")}&background=random&size=200",
                        contentDescription = "Profile picture of ${worker.name}",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Availability Badge
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .background(
                                if (worker.isAvailable) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                                CircleShape
                            )
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = TranslationUtils.getTranslatedName(worker.name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Skills (Max 3)
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        worker.skillsList.take(3).forEach { skill ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ) {
                                SkillChipLabel(
                                    skill = skill,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Location
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = TranslationUtils.getTranslatedArea(worker.area),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Text(
                        text = "₹${worker.dailyWage.toInt()}${stringResource(R.string.per_day)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = saffronColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Call Button
                IconButton(
                    onClick = { showCallDialog = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = saffronColor
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call ${worker.name}"
                    )
                }
            }

            // Rating and Rate Button Section
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = Color.LightGray.copy(alpha = 0.5f)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.worker_card_rating_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    RatingBar(
                        rating = worker.averageRating,
                        totalRatings = worker.totalRatings
                    )
                }

                val rateButtonEnabled = !hasRatedToday
                
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        if (hasRatedToday) {
                            PlainTooltip { Text(stringResource(R.string.worker_card_already_rated)) }
                        }
                    },
                    state = rememberTooltipState()
                ) {
                    Button(
                        onClick = { showRateSheet = true },
                        enabled = rateButtonEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (rateButtonEnabled) Color(0xFFF5F5F5) else Color(0xFFE0E0E0),
                            contentColor = if (rateButtonEnabled) Color.Black else Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (rateButtonEnabled) saffronColor else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasRatedToday) stringResource(R.string.worker_card_rated) else stringResource(R.string.worker_card_rate), 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
