# Guion de Presentación del Proyecto (TFG)

**Duración estimada:** 3 - 4 minutos.  
**Requisitos previos:**
*   Tener la aplicación iniciada.
*   Tener abiertos dos navegadores (o ventana normal e incógnito) para simular la interacción entre dos usuarios.
*   Tener creados previamente los usuarios de prueba en la base de datos (ya que no mostraremos el registro público).

---

## 1. Introducción y Acceso Seguro (0:00 - 0:45)

**[Pantalla de Login]**
*   **Acción:** Muestra la pantalla de inicio de sesión vacía.
*   **Locución:** "Hola, soy [Tu Nombre] y voy a presentar mi proyecto integrado: una plataforma web de comunicación corporativa y gestión de eventos en tiempo real."
*   **Acción:** Introduce las credenciales de un usuario estándar (ej: `usuario1`).
*   **Locución:** "El acceso a la aplicación es restringido y requiere credenciales previamente asignadas por un administrador, garantizando que solo el personal autorizado pueda acceder al sistema."
*   **Acción:** Haz clic en **Login** y espera a que cargue la pantalla principal.
*   **Locución:** "La autenticación se gestiona de forma segura mediante Spring Security, utilizando contraseñas encriptadas."

---

## 2. Chat y Comunicación en Tiempo Real (0:45 - 2:00)

**[Vista Principal / Chat]**
*   **Acción:** Señala con el ratón la lista de contactos a la izquierda y el área de chat vacía o con historial previo.
*   **Locución:** "Una vez dentro, encontramos la interfaz principal. A la izquierda disponemos de la lista de compañeros y grupos de trabajo, y a la derecha el panel de conversación."

**[Demostración de Chat]**
*   **Acción:** Selecciona un contacto y escribe: *"Hola, ¿estás disponible para revisar el informe?"*.
*   **Locución:** "El núcleo del proyecto es la comunicación en tiempo real basada en WebSockets. Como se puede observar, el envío de mensajes es instantáneo."
*   **Acción:** *(Opcional)* Muestra brevemente la otra ventana del navegador donde llega el mensaje automáticamente.
*   **Locución:** "La recepción por parte del destinatario es inmediata, sin necesidad de recargar la página."

**[Multimedia y Grupos]**
*   **Acción:** Envía un emoji o adjunta una imagen de prueba.
*   **Locución:** "El sistema no se limita a texto plano; permite una comunicación rica mediante el uso de emojis y el intercambio de archivos e imágenes."
*   **Acción:** Crea un nuevo grupo, ponle nombre (ej: "Equipo Desarrollo") y añade dos usuarios.
*   **Locución:** "Para facilitar el trabajo en equipo, también es posible crear salas de chat grupales dinámicas, donde múltiples usuarios pueden colaborar simultáneamente."

---

## 3. Gestión de Eventos y Calendario (2:00 - 3:00)

**[Módulo de Eventos]**
*   **Acción:** Navega a la sección de **"Eventos"** o **"Calendario"**.
*   **Locución:** "Complementando la comunicación, la aplicación integra un módulo de organización y planificación."
*   **Acción:** Selecciona un día y crea un evento (Título: "Reunión Semanal", Color: Azul).
*   **Locución:** "Los usuarios pueden gestionar una agenda compartida creando eventos. Podemos definir la fecha, hora y categorizarlos visualmente mediante colores."
*   **Acción:** Guarda el evento y muéstralo reflejado en el calendario.
*   **Locución:** "Esta funcionalidad permite coordinar reuniones y plazos de entrega de manera visual, centralizando la organización del equipo en la misma plataforma."

---

## 4. Panel de Administración y Auditoría (3:00 - 4:00)

**[Cambio a Rol Administrador]**
*   **Acción:** Cierra sesión (`Logout`) e inicia sesión con una cuenta de **Administrador**.
*   **Locución:** "Para la gestión del sistema, contamos con un perfil de Administrador con privilegios elevados."

**[Gestión de Usuarios]**
*   **Acción:** Entra en el panel de **Administración de Usuarios**.
*   **Locución:** "Desde aquí, el administrador tiene control total sobre los usuarios: puede darlos de alta, asignar roles o bloquear el acceso si fuera necesario."

**[Auditoría (Audit Logs)]**
*   **Acción:** Navega a la pestaña de **Auditoría**. Muestra la tabla de registros.
*   **Locución:** "Un aspecto crítico es la seguridad y trazabilidad. El sistema incluye un módulo de auditoría que registra automáticamente todas las acciones relevantes."
*   **Acción:** Señala un registro reciente (ej: el login que acabas de hacer).
*   **Locución:** "Podemos inspeccionar quién ha accedido al sistema, qué cambios se han realizado y desde qué dirección IP, proporcionando un control exhaustivo sobre la actividad de la plataforma."

---

## 5. Conclusión (4:00 - Fin)

**[Cierre]**
*   **Acción:** Vuelve a la pantalla de inicio o perfil del administrador.
*   **Locución:** "En definitiva, este proyecto ofrece una solución robusta y escalable para la comunicación interna y la gestión de tareas, combinando tecnologías modernas de desarrollo web."
*   **Locución:** "Muchas gracias por su atención."
