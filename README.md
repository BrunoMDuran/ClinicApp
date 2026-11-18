# ClinicApp

Aplicación de escritorio en Java (Swing) para la gestión de una clínica: pacientes, médicos, especialidades, usuarios, citas e historia clínica. Fue el trabajo final de una materia de la carrera y se conecta a una base de datos SQL Server.

## Tecnologías

- Java 8 o superior, interfaz Swing, proyecto de NetBeans (Ant)
- SQL Server mediante JDBC (`mssql-jdbc`); también se incluye el conector de MySQL en `lib/`

## Estructura

- `src/gui`: formularios de la interfaz
- `src/modelo`: clases del dominio
- `src/conexion`: acceso a la base de datos
- `src/hilos`: tareas en segundo plano
- `docs/`: informe del trabajo final

## Configuración de la base de datos

La conexión se configura con variables de entorno:

| Variable | Valor por defecto |
|---|---|
| `CLINICA_DB_HOST` | `localhost:1433` |
| `CLINICA_DB_NAME` | `ClinicaDB` |
| `CLINICA_DB_USER` | `sa` |
| `CLINICA_DB_PASSWORD` | vacío |

El repositorio no incluye el script de creación de la base de datos.

## Ejecución

Abrir la carpeta como proyecto en NetBeans y ejecutar `main.MainApp`, o compilar con `ant jar`.
