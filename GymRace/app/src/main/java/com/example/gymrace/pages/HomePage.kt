package np.com.bimalkafle.bottomnavigationdemo.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.gymrace.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomePage(modifier: Modifier = Modifier) {

    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
//            .background(Color(0xFF121212)), // Color de fondo oscuro
//        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(1) {

            Spacer(modifier = Modifier.height(64.dp))

            Text(
                text = "Bienvenido a Gym Race", //nombre del usuario falta
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Rutina predefinidas",
                fontSize = 18.sp,
                color = Color.Gray

            )

            Spacer(modifier = Modifier.height(10.dp))








            LazyRow() {
                items(10) { index -> // Rutinas
                    Button(
                        onClick = { /* Navegar a la página de rutinas */ },
                        modifier = Modifier
                            .padding(16.dp)
                            .height(200.dp)
                            .width(250.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(Color(0xff505050))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Músculo $index", // Aquí pondrías el nombre del músculo
                                color = Color.White,
                                fontSize = 20.sp,
                                modifier = Modifier.padding(start = 16.dp)
                            )

                            Image(
                                painter = painterResource(id = R.drawable.fitness), //Reemplaza con la imagen correcta
                                contentDescription = "Imagen músculo",
                                modifier = Modifier
                                    .size(120.dp)
                                    .padding(end = 16.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }














            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Crea tu rutina personalizada",
                fontSize = 18.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(10.dp))
            LazyRow() {
                items(1) {
                    Button(
                        onClick = { /* Navegar a la página de rutinas */ },
                        modifier = Modifier
                            .padding(16.dp)
                            .height(200.dp)
                            .width(200.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(Color(0xFFE91E63))
                    ) {
                        Text(text = "+", color = Color.White, fontSize = 35.sp)
                    }            }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Crea tu rutina personalizada",
                fontSize = 18.sp,
                color = Color.Gray
            )
            Button(
                onClick = { /* Navegar a la página de rutinas */ },
                modifier = Modifier
                    .padding(16.dp)
                    .height(200.dp)
                    .width(200.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFFE91E63))
            ) {
                Text(text = "+", color = Color.White, fontSize = 35.sp)
            }
            Spacer(modifier = Modifier.height(95.dp)) // Add space below the last exercise

        }
    }

}