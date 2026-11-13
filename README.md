# Proyecto IoT: Integracion de ESP32 con aplicacion android segura (Eva 3)

## Integrantes:

- Daniel Jara
- William Gómez

## 1. Descripcion del sistema

Este proyecto implementa una arquitectura IoT simulada en Hertzbench, integrando un nodo microcontrolador ESP32 y una aplicación móvil Android (ControlIoT). El sistema permite monitorear la temperatura ambiental mediante un sensor analógico (TMP36) y accionar de forma remota un sistema de ventilacion (servomotor) a traves de peticiones HTTP sobre red Wi-Fi

## 2. Protocolo de comunicacion y seguridad (ISO 27000)

Para garantizar la integridad, autenticidad y no repudio de las órdenes enviadas al actuador, la comunicación implementa los siguientes mecanismos criptograficos:

- Estructura del mensaje: Las órdenes siguen el formato plano `CMD;<accion>;<n>` (ej. `CMD;ON;1731450001`)
- Firma Digital HMAC-SHA256: La aplicación Android calcula un código de autenticación de mensaje utilizando una clave secreta compartida (`clave-compartida-demo`). Antes de ejecutar cualquier acción, el firmware del ESP32 recalcula el hash y lo compara con el parámetro `sig` recibido. Si no coinciden, la petición se descarta inmediatamente
- Proteccion anti-repeticion: Cada solicitud incluye un contador incremental (`n`). El ESP32 almacena el último valor procesado (`ultimoContador`) y rechaza cualquier paquete cuyo contador sea menor o igual al registrado, mitigando ataques de retransmisión

## 3. Control de acceso y roles de usuario

La aplicación movil integra un sistema de autenticacion local gestionado mediante SQLite (heredado de la Unidad 2):

- Control de sesion: Los usuarios deben autenticarse en `MainActivity` antes de acceder al panel de control (`DashboardActivity`)
- Trazabilidad: Las lecturas de temperatura y estados del actuador pueden respaldarse localmente en la base de datos SQLite para auditoría operativa

---

## 4. Endpoints implementados en el firmware

- `GET /cmd?accion=<ON|OFF>&n=<contador>&sig=<hmac>`: Recibe, valida y ejecuta el encendido o apagado del servomotor
- `GET /lectura`: Devuelve en formato JSON la temperatura actual calculada y el estado lógico del ventilador

---

## 5. Matriz de pruebas y resultados esperados

| Caso de Prueba           | Acción Ejecutada                                                  | Validación en ESP32                    | Respuesta HTTP                        | Registro en Log (`HZB` / Serial)     |
| :----------------------- | :---------------------------------------------------------------- | :------------------------------------- | :------------------------------------ | :----------------------------------- |
| 1- Comando legitimo      | Envío de `ON` u `OFF` con firma HMAC válida y contador nuevo.     | Firma coincide y `n > ultimoContador`. | `200 OK` (`ACK;<accion>; OK`)         | `cmd OK ON` / `cmd ON -> 200 ACK`    |
| 2- Firma adulterada      | Envío de comando mediante el botón de prueba con hash modificado. | El hash recalculado difiere de `sig`.  | `401 Unauthorized` (`firma invalida`) | `seguridad RECHAZADO firma invalida` |
| 3- Ataque de repeticion  | Reenvío de una petición válida con un contador `n` ya utilizado.  | `n <= ultimoContador`.                 | `409 Conflict` (`repetido`)           | `seguridad RECHAZADO repeticion`     |
| 4- Lectura de telemetria | Consulta desde la app al endpoint `/lectura`.                     | Lectura analógica del pin del sensor.  | `200 OK` (JSON)                       | `lectura -> 200 {"temperatura":...}` |

## No hemos podido ejecutar las pruebas. Asi que el punto 5 es como una aproximacion o estimacion de lo que esperamos que responda (T-T)
