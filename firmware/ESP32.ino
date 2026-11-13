#include <WiFi.h>
#include <WebServer.h>
#include <Servo.h>
#include "mbedtls/md.h"


const char *CLAVE = "clave-compartida-demo"; 
WebServer server(80);
unsigned long ultimoContador = 0;


const int sensorPin = 34; 
Servo ventiladorServo;
const int servoPin = 9;   
bool encendido = false;


String hmac(const String &msg) {
  byte out[32];
  mbedtls_md_context_t c;
  mbedtls_md_init(&c);
  mbedtls_md_setup(&c, mbedtls_md_info_from_type(MBEDTLS_MD_SHA256), 1);
  mbedtls_md_hmac_starts(&c, (const byte *)CLAVE, strlen(CLAVE));
  mbedtls_md_hmac_update(&c, (const byte *)msg.c_str(), msg.length());
  mbedtls_md_hmac_finish(&c, out);
  mbedtls_md_free(&c);

  String hex = "";
  for (int i = 0; i < 32; i++) {
    char b[3];
    sprintf(b, "%02x", out[i]);
    hex += b;
  }
  return hex;
}


void handleCmd() {
  if (!server.hasArg("accion") || !server.hasArg("n") || !server.hasArg("sig")) {
    server.send(400, "text/plain", "parametros incompletos\n");
    return;
  }

  String accion = server.arg("accion");
  unsigned long n = server.arg("n").toInt();
  String firma = server.arg("sig");


  String msg = "CMD;" + accion + ";" + String(n);


  if (firma != hmac(msg)) {
    Serial.println("seguridad RECHAZADO firma invalida " + msg);
    server.send(401, "text/plain", "firma invalida\n");
    return;
  }


  if (n <= ultimoContador) {
    Serial.println("seguridad RECHAZADO repeticion " + msg);
    server.send(409, "text/plain", "repetido\n");
    return;
  }

  ultimoContador = n;
  Serial.println("cmd OK " + accion);


  if (accion == "ON" && !encendido) {
    ventiladorServo.write(180);
    encendido = true;
  } 
  else if (accion == "OFF" && encendido) {
    ventiladorServo.write(0);
    encendido = false;
  }

  server.send(200, "text/plain", "ACK;" + accion + "; OK\n");
}


void handleLectura() {
  int lectura = analogRead(sensorPin);
  float voltaje = lectura * 3.3 / 4095.0; // Rango de voltaje de entrada en ESP32 emulado
  float temperatura = (voltaje - 0.5) * 100.0;

  String jsonRespuesta = "{\"temperatura\": " + String(temperatura, 2) + ", \"estado\": \"" + (encendido ? "ON" : "OFF") + "\"}";
  server.send(200, "application/json", jsonRespuesta);
}

void setup() {
  Serial.begin(115200);


  ventiladorServo.attach(servoPin);
  ventiladorServo.write(0);

  WiFi.begin("Espressif", "", 5);
  while (WiFi.status() != WL_CONNECTED) {
    delay(100);
  }

  server.on("/cmd", handleCmd);
  server.on("/lectura", handleLectura);
  server.begin();
  
  Serial.println("http listening on 80");
}

void loop() {
  server.handleClient();
}