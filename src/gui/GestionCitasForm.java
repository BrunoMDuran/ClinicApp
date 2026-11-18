package gui;

import conexion.ConexionSQLServer;
import modelo.Cita;
import modelo.Paciente;
import modelo.Medico;
import modelo.HorarioMedico;
import modelo.Especialidad;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.awt.Image;
import java.io.IOException;
import javax.imageio.ImageIO;

public class GestionCitasForm extends javax.swing.JFrame {

    private ConexionSQLServer conexion;
    private List<Paciente> pacientesDisponibles;
    private List<Medico> medicosDisponibles;
    private List<Especialidad> especialidadesDisponibles;
    private DefaultTableModel tablaCitasModel; // ¡Ojo con este!
    private Paciente pacienteSeleccionado = null;

    public GestionCitasForm() {
        initComponents();
    }

    private void inicializarModeloTabla() {
        tablaCitasModel = new DefaultTableModel(
                new Object[]{"ID Cita", "Fecha", "Hora", "Paciente", "Medico", "Motivo", "Estado"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblCitas.setModel(tablaCitasModel);
    }
    
    public GestionCitasForm(ConexionSQLServer conexion) {
        initComponents();

        this.conexion = conexion;
        this.pacientesDisponibles = new ArrayList<>();
        this.medicosDisponibles = new ArrayList<>();
        this.especialidadesDisponibles = new ArrayList<>();
        setLocationRelativeTo(null);

        setTitle("Gestion de Citas");        
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(null);
        try {
            Image icon = ImageIO.read(getClass().getResource("/resources/logo.png"));
            this.setIconImage(icon); // 'this' se refiere al JFrame actual
        } catch (IOException e) {
            System.err.println("ERROR: No se pudo cargar el logo: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: La ruta del logo es invalida o el archivo no existe: " + e.getMessage());
        }
        inicializarModeloTabla();
        cargarDatosIniciales();
        cargarCitasEnTabla(null, null);
    }

    private void cargarDatosIniciales() {
        cmbEspecialidadFiltro.addItem("Todas las Especialidades");
        String queryEsp = "SELECT IdEspecialidad, NombreEspecialidad FROM Especialidades ORDER BY NombreEspecialidad";
        Connection conn = null;
        Statement stm = null;
        ResultSet rs = null;
        try {
            conn = conexion.getConexion();
            if (conn == null) {
                throw new SQLException("No hay conexion a la base de datos.");
            }
            stm = conn.createStatement();
            rs = stm.executeQuery(queryEsp);
            while (rs.next()) {
                Especialidad e = new Especialidad(rs.getInt("IdEspecialidad"), rs.getString("NombreEspecialidad"));
                especialidadesDisponibles.add(e);
                cmbEspecialidadFiltro.addItem(e.getNombre());
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al cargar especialidades: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error al cargar especialidades.", "Error DB", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stm != null) {
                    stm.close();
                }
            } catch (SQLException ex) {
                System.err.println("ERROR: Fallo al cerrar recursos JDBC: " + ex.getMessage());
            }
        }

        cargarMedicosPorEspecialidad();
    }

    private void buscarPaciente() {
        try {
            int dni = Integer.parseInt(txtPacienteDniBusqueda.getText());
            String query = "SELECT IdPaciente, Dni, Nombre, Apellido FROM Pacientes WHERE Dni = ?";
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                conn = conexion.getConexion();
                if (conn == null) {
                    throw new SQLException("No hay conexion a la base de datos.");
                }
                pstmt = conn.prepareStatement(query);

                pstmt.setInt(1, dni);
                rs = conexion.ejecutarQuery(pstmt);
                if (rs.next()) {
                    pacienteSeleccionado = new Paciente(rs.getInt("IdPaciente"), rs.getInt("Dni"), rs.getString("Nombre"), rs.getString("Apellido"), null, null, null, null);
                    lblPacienteSeleccionado.setText("Paciente Seleccionado: " + pacienteSeleccionado.getNombre() + " " + pacienteSeleccionado.getApellido() + " (DNI: " + pacienteSeleccionado.getDni() + ")");
                    JOptionPane.showMessageDialog(this, "Paciente encontrado.", "Busqueda Exitosa", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    pacienteSeleccionado = null;
                    lblPacienteSeleccionado.setText("Paciente Seleccionado: Ninguno");
                    JOptionPane.showMessageDialog(this, "Paciente no encontrado con DNI: " + dni, "No Encontrado", JOptionPane.WARNING_MESSAGE);
                }
            } catch (SQLException e) {
                System.err.println("ERROR: Fallo al buscar paciente: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error de BD al buscar paciente.", "Error DB", JOptionPane.ERROR_MESSAGE);
            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (pstmt != null) {
                        pstmt.close();
                    }
                } catch (SQLException ex) {
                    System.err.println("ERROR: Fallo al cerrar recursos JDBC: " + ex.getMessage());
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Ingrese un DNI valido para buscar.", "Error de DNI", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarMedicosPorEspecialidad() {
        cmbMedico.removeAllItems();
        cmbMedico.addItem("Seleccionar Medico");
        medicosDisponibles.clear();

        String especialidadSeleccionada = (String) cmbEspecialidadFiltro.getSelectedItem();
        String query = "SELECT m.IdMedico, m.Nombre, m.Apellido, m.NumeroMatricula, m.Telefono, e.IdEspecialidad, e.NombreEspecialidad FROM Medicos m JOIN Especialidades e ON m.IdEspecialidad = e.IdEspecialidad";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = conexion.getConexion();
            if (conn == null) {
                throw new SQLException("No hay conexion a la base de datos.");
            }

            if (especialidadSeleccionada != null && !especialidadSeleccionada.equals("Todas las Especialidades")) {
                pstmt = conn.prepareStatement(query + " WHERE e.NombreEspecialidad = ? ORDER BY m.Apellido, m.Nombre");
                pstmt.setString(1, especialidadSeleccionada);
            } else {
                pstmt = conn.prepareStatement(query + " ORDER BY m.Apellido, m.Nombre");
            }

            rs = conexion.ejecutarQuery(pstmt);

            while (rs.next()) {
                Especialidad esp = new Especialidad(rs.getInt("IdEspecialidad"), rs.getString("NombreEspecialidad"));
                Medico m = new Medico(rs.getInt("IdMedico"), rs.getString("Nombre"), rs.getString("Apellido"), rs.getString("NumeroMatricula"), esp, rs.getString("Telefono"));
                medicosDisponibles.add(m);
                cmbMedico.addItem("Dr/a. " + m.getApellido() + " (" + m.getEspecialidad().getNombre() + ")");
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al cargar medicos: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error al cargar medicos.", "Error DB", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException ex) {
                System.err.println("ERROR: Fallo al cerrar recursos JDBC: " + ex.getMessage());
            }
        }
        cargarProximasFechasDisponibles();
    }

    private void cargarProximasFechasDisponibles() {
        cmbProximasFechas.removeAllItems();
        cmbProximasFechas.addItem("Seleccionar Fecha Sugerida");

        int selectedMedicoIndex = cmbMedico.getSelectedIndex();
        if (selectedMedicoIndex <= 0) {
            txtFechaCita.setText("");
            cmbHoraCita.removeAllItems();
            cmbHoraCita.addItem("Seleccionar Hora");
            return;
        }
        Medico medicoSeleccionado = medicosDisponibles.get(selectedMedicoIndex - 1);

        List<HorarioMedico> horariosSemanales = obtenerHorariosSemanalesMedico(medicoSeleccionado);
        if (horariosSemanales.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El medico seleccionado no tiene horarios configurados para la semana.", "Sin Horarios", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate fechaActual = LocalDate.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < 60; i++) {
            LocalDate fechaAProbar = fechaActual.plusDays(i);
            String diaSemanaSQL = fechaAProbar.getDayOfWeek().toString();
            switch (diaSemanaSQL) {
                case "MONDAY":
                    diaSemanaSQL = "Lunes";
                    break;
                case "TUESDAY":
                    diaSemanaSQL = "Martes";
                    break;
                case "WEDNESDAY":
                    diaSemanaSQL = "Miercoles";
                    break;
                case "THURSDAY":
                    diaSemanaSQL = "Jueves";
                    break;
                case "FRIDAY":
                    diaSemanaSQL = "Viernes";
                    break;
                case "SATURDAY":
                    diaSemanaSQL = "Sabado";
                    break;
                case "SUNDAY":
                    diaSemanaSQL = "Domingo";
                    break;
            }

            for (HorarioMedico hm : horariosSemanales) {
                if (hm.getDiaSemana().equals(diaSemanaSQL)) {
                    if (hayTurnoDisponibleEnFecha(medicoSeleccionado.getIdMedico(), fechaAProbar, hm.getHoraInicio(), hm.getHoraFin())) {
                        cmbProximasFechas.addItem(fechaAProbar.format(dateFormatter));
                        break;
                    }
                }
            }
        }
    }

    private List<HorarioMedico> obtenerHorariosSemanalesMedico(Medico medico) {
        List<HorarioMedico> horarios = new ArrayList<>();
        String query = "SELECT IdHorario, DiaSemana, HoraInicio, HoraFin FROM HorariosMedicos WHERE IdMedico = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = conexion.getConexion();
            if (conn == null) {
                throw new SQLException("No hay conexion a la base de datos.");
            }
            pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, medico.getIdMedico());
            rs = conexion.ejecutarQuery(pstmt);
            while (rs.next()) {
                horarios.add(new HorarioMedico(
                        rs.getInt("IdHorario"),
                        medico,
                        rs.getString("DiaSemana"),
                        rs.getTime("HoraInicio").toLocalTime(),
                        rs.getTime("HoraFin").toLocalTime()
                ));
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al obtener horarios semanales del medico: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error al cargar horarios semanales.", "Error DB", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException ex) {
                System.err.println("ERROR: Fallo al cerrar recursos JDBC: " + ex.getMessage());
            }
        }
        return horarios;
    }

    private boolean hayTurnoDisponibleEnFecha(int idMedico, LocalDate fecha, LocalTime horaInicioFranja, LocalTime horaFinFranja) {
        LocalTime horaActual = horaInicioFranja;
        while (horaActual.isBefore(horaFinFranja)) {
            if (estaHoraDisponible(idMedico, fecha, horaActual)) {
                return true;
            }
            horaActual = horaActual.plusMinutes(30);
        }
        return false;
    }

    private void cargarHorasDisponibles() {
        cmbHoraCita.removeAllItems();
        cmbHoraCita.addItem("Seleccionar Hora");

        int selectedMedicoIndex = cmbMedico.getSelectedIndex();
        if (selectedMedicoIndex <= 0) {
            return;
        }
        Medico medicoSeleccionado = medicosDisponibles.get(selectedMedicoIndex - 1);

        LocalDate fechaCita;
        try {
            fechaCita = LocalDate.parse(txtFechaCita.getText());
        } catch (DateTimeParseException e) {
            return;
        }

        String diaSemana = fechaCita.getDayOfWeek().toString();
        switch (diaSemana) {
            case "MONDAY":
                diaSemana = "Lunes";
                break;
            case "TUESDAY":
                diaSemana = "Martes";
                break;
            case "WEDNESDAY":
                diaSemana = "Miercoles";
                break;
            case "THURSDAY":
                diaSemana = "Jueves";
                break;
            case "FRIDAY":
                diaSemana = "Viernes";
                break;
            case "SATURDAY":
                diaSemana = "Sabado";
                break;
            case "SUNDAY":
                diaSemana = "Domingo";
                break;
        }

        List<HorarioMedico> horariosMedico = obtenerHorariosSemanalesMedico(medicoSeleccionado);

        for (HorarioMedico hm : horariosMedico) {
            if (hm.getDiaSemana().equals(diaSemana)) {
                LocalTime horaActual = hm.getHoraInicio();
                while (horaActual.isBefore(hm.getHoraFin())) {
                    if (estaHoraDisponible(medicoSeleccionado.getIdMedico(), fechaCita, horaActual)) {
                        cmbHoraCita.addItem(horaActual.format(DateTimeFormatter.ofPattern("HH:mm")));
                    }
                    horaActual = horaActual.plusMinutes(30);
                }
            }
        }
    }

    private boolean estaHoraDisponible(int idMedico, LocalDate fecha, LocalTime hora) {
        String query = "SELECT COUNT(*) FROM Citas WHERE IdMedico = ? AND FechaCita = ? AND CAST(HoraCita AS TIME) = CAST(? AS TIME) AND Estado != 'Cancelada'";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = conexion.getConexion();
            if (conn == null) {
                throw new SQLException("No hay conexion a la base de datos.");
            }
            pstmt = conn.prepareStatement(query);

            pstmt.setInt(1, idMedico);
            pstmt.setDate(2, java.sql.Date.valueOf(fecha));
            pstmt.setObject(3, java.sql.Time.valueOf(hora), java.sql.Types.TIME);

            rs = conexion.ejecutarQuery(pstmt);
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al verificar disponibilidad de hora: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error al verificar disponibilidad de hora.", "Error DB", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException ex) {
                System.err.println("ERROR: Fallo al cerrar recursos JDBC: " + ex.getMessage());
            }
        }
        return false;
    }

    private void agendarNuevaCita() {
        if (pacienteSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Busque y seleccione un paciente primero.", "Paciente No Seleccionado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int selectedMedicoIndex = cmbMedico.getSelectedIndex();
        if (selectedMedicoIndex <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un medico.", "Medico No Seleccionado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Medico medico = medicosDisponibles.get(selectedMedicoIndex - 1);

        LocalDate fechaCita;
        try {
            fechaCita = LocalDate.parse(txtFechaCita.getText());
            if (fechaCita.isBefore(LocalDate.now())) {
                JOptionPane.showMessageDialog(this, "La fecha de la cita no puede ser en el pasado.", "Fecha Invalida", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Formato de fecha invalido (AAAA-MM-DD).", "Error de Fecha", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int selectedHoraIndex = cmbHoraCita.getSelectedIndex();
        if (selectedHoraIndex <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una hora para la cita.", "Hora No Seleccionada", JOptionPane.WARNING_MESSAGE);
            return;
        }
        LocalTime horaCita = LocalTime.parse((String) cmbHoraCita.getSelectedItem());

        String motivo = txtMotivoCita.getText().trim();
        if (motivo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese el motivo de la cita.", "Motivo Vacio", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!estaHoraDisponible(medico.getIdMedico(), fechaCita, horaCita)) {
            JOptionPane.showMessageDialog(this, "La hora seleccionada ya no esta disponible para este medico. Refresque las horas.", "Hora No Disponible", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Cita nuevaCita = new Cita(pacienteSeleccionado, medico, fechaCita, horaCita, motivo);

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rsGeneratedKeys = null;
        try {
            conn = conexion.getConexion();
            if (conn == null) {
                throw new SQLException("No hay conexion a la base de datos.");
            }
            conn.setAutoCommit(false);

            String insertSQL = "INSERT INTO Citas (IdPaciente, IdMedico, FechaCita, HoraCita, Motivo, Estado) VALUES (?, ?, ?, ?, ?, ?)";
            pstmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(1, nuevaCita.getPaciente().getIdPaciente());
            pstmt.setInt(2, nuevaCita.getMedico().getIdMedico());
            pstmt.setDate(3, java.sql.Date.valueOf(nuevaCita.getFechaCita()));
            pstmt.setObject(4, java.sql.Time.valueOf(nuevaCita.getHoraCita()), java.sql.Types.TIME);
            pstmt.setString(5, nuevaCita.getMotivo());
            pstmt.setString(6, nuevaCita.getEstado());
            conexion.ejecutarUpdate(pstmt);

            rsGeneratedKeys = pstmt.getGeneratedKeys();
            if (rsGeneratedKeys.next()) {
                nuevaCita.setIdCita(rsGeneratedKeys.getInt(1));
            }
            conn.commit();
            JOptionPane.showMessageDialog(this, "Cita agendada con exito. ID: " + nuevaCita.getIdCita(), "Exito", JOptionPane.INFORMATION_MESSAGE);
            limpiarCamposCita();
            cargarCitasEnTabla(null, null);
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al agendar cita en BD: " + e.getMessage());
            try {
                if (conn != null && !conn.isClosed()) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        System.err.println("ERROR: Fallo en rollback: " + ex.getMessage());
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(GestionCitasForm.class.getName()).log(Level.SEVERE, null, ex);
            }
            JOptionPane.showMessageDialog(this, "Error al agendar cita: " + e.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (rsGeneratedKeys != null) {
                    rsGeneratedKeys.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException ex) {
                System.err.println("ERROR: Fallo al cerrar recursos JDBC: " + ex.getMessage());
            }
            try {
                if (conn != null && !conn.isClosed()) {
                    try {
                        conn.setAutoCommit(true);
                    } catch (SQLException ex) {
                        System.err.println("ERROR: Fallo al restaurar auto-commit: " + ex.getMessage());
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(GestionCitasForm.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void cambiarEstadoCitaSeleccionada(String nuevoEstado) {
        int selectedRow = tblCitas.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione una cita de la tabla.", "Sin Seleccion", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int idCita = (Integer) tablaCitasModel.getValueAt(selectedRow, 0);
        String estadoActual = ((String) tablaCitasModel.getValueAt(selectedRow, 6)).trim();

        if (estadoActual.equals(nuevoEstado)) {
            JOptionPane.showMessageDialog(this, "La cita ya esta en estado '" + nuevoEstado + "'.", "Estado Invalido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if ("Cancelada".equals(estadoActual) || "Atendida".equals(estadoActual) || "No Asistida".equals(estadoActual)) {
            JOptionPane.showMessageDialog(this, "No se puede cambiar el estado de una cita que ya esta en estado final (Cancelada/Atendida/No Asistida).", "Estado Final", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = conexion.getConexion();
            if (conn == null) {
                throw new SQLException("No hay conexion a la base de datos.");
            }
            conn.setAutoCommit(false);

            String updateSQL = "UPDATE Citas SET Estado = ? WHERE IdCita = ?";
            pstmt = conn.prepareStatement(updateSQL);
            pstmt.setString(1, nuevoEstado);
            pstmt.setInt(2, idCita);
            conexion.ejecutarUpdate(pstmt);
            conn.commit();
            JOptionPane.showMessageDialog(this, "Cita ID " + idCita + " marcada como " + nuevoEstado + ".", "Estado Actualizado", JOptionPane.INFORMATION_MESSAGE);
            cargarCitasEnTabla(null, null);
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al actualizar estado de cita en BD: " + e.getMessage());
            try {
                if (conn != null && !conn.isClosed()) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        System.err.println("ERROR: Fallo en rollback: " + ex.getMessage());
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(GestionCitasForm.class.getName()).log(Level.SEVERE, null, ex);
            }
            JOptionPane.showMessageDialog(this, "Error al actualizar estado de cita: " + e.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException ex) {
                System.err.println("ERROR: Fallo al cerrar recursos JDBC: " + ex.getMessage());
            }
            try {
                if (conn != null && !conn.isClosed()) {
                    try {
                        conn.setAutoCommit(true);
                    } catch (SQLException ex) {
                        System.err.println("ERROR: Fallo al restaurar auto-commit: " + ex.getMessage());
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(GestionCitasForm.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void cargarCitasEnTabla(Integer idMedico, LocalDate fecha) {
        tablaCitasModel.setRowCount(0);

        StringBuilder queryBuilder = new StringBuilder("SELECT c.IdCita, c.FechaCita, c.HoraCita, c.Motivo, c.Estado, p.Nombre AS PacienteNombre, p.Apellido AS PacienteApellido, m.Nombre AS MedicoNombre, m.Apellido AS MedicoApellido FROM Citas c JOIN Pacientes p ON c.IdPaciente = p.IdPaciente JOIN Medicos m ON c.IdMedico = m.IdMedico");

        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        if (idMedico != null) {
            conditions.add("c.IdMedico = ?");
            params.add(idMedico);
        }
        if (fecha != null) {
            conditions.add("c.FechaCita = ?");
            params.add(java.sql.Date.valueOf(fecha));
        }

        if (!conditions.isEmpty()) {
            queryBuilder.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        queryBuilder.append(" ORDER BY c.FechaCita ASC, c.HoraCita ASC");

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = conexion.getConexion();
            if (conn == null) {
                throw new SQLException("No hay conexion a la base de datos.");
            }
            pstmt = conn.prepareStatement(queryBuilder.toString());

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            rs = conexion.ejecutarQuery(pstmt);
            while (rs.next()) {
                tablaCitasModel.addRow(new Object[]{
                    rs.getInt("IdCita"),
                    rs.getDate("FechaCita").toLocalDate(),
                    rs.getTime("HoraCita").toLocalTime(),
                    rs.getString("PacienteNombre") + " " + rs.getString("PacienteApellido"),
                    rs.getString("MedicoNombre") + " " + rs.getString("MedicoApellido"),
                    rs.getString("Motivo"),
                    rs.getString("Estado")
                });
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al cargar citas en tabla: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error al cargar citas.", "Error DB", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException ex) {
                System.err.println("ERROR: Fallo al cerrar recursos JDBC: " + ex.getMessage());
            }
        }
    }

    private void limpiarCamposCita() {
        cmbEspecialidadFiltro.setSelectedIndex(0);
        cmbMedico.setSelectedIndex(0);
        cmbProximasFechas.setSelectedIndex(0);
        txtFechaCita.setText("");
        cmbHoraCita.removeAllItems();
        cmbHoraCita.addItem("Seleccionar Hora");
        txtMotivoCita.setText("");
        pacienteSeleccionado = null;
        lblPacienteSeleccionado.setText("Paciente Seleccionado: Ninguno");
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpPaciente = new javax.swing.JPanel();
        lblPaciente = new javax.swing.JLabel();
        lblDniPaciente = new javax.swing.JLabel();
        txtPacienteDniBusqueda = new javax.swing.JTextField();
        btnBuscarPaciente = new javax.swing.JButton();
        lblPacienteSeleccionado = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        cmbEspecialidadFiltro = new javax.swing.JComboBox<>();
        cmbMedico = new javax.swing.JComboBox<>();
        cmbProximasFechas = new javax.swing.JComboBox<>();
        cmbHoraCita = new javax.swing.JComboBox<>();
        txtFechaCita = new javax.swing.JTextField();
        txtMotivoCita = new javax.swing.JTextField();
        btnAgendarCita = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblCitas = new javax.swing.JTable();
        btnCancelarCita = new javax.swing.JButton();
        btnMarcarAtendida = new javax.swing.JButton();
        btnRefrescarCitas = new javax.swing.JButton();
        btnMarcarNoAsistida = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Gestion de Citas");
        setMaximumSize(new java.awt.Dimension(1000, 780));
        setMinimumSize(new java.awt.Dimension(1000, 780));
        setResizable(false);
        setSize(new java.awt.Dimension(1000, 780));

        lblPaciente.setText("Paciente:");

        lblDniPaciente.setText("DNI Paciente:");

        btnBuscarPaciente.setText("Buscar Paciente");
        btnBuscarPaciente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBuscarPacienteActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jpPacienteLayout = new javax.swing.GroupLayout(jpPaciente);
        jpPaciente.setLayout(jpPacienteLayout);
        jpPacienteLayout.setHorizontalGroup(
            jpPacienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpPacienteLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpPacienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpPacienteLayout.createSequentialGroup()
                        .addComponent(lblDniPaciente)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtPacienteDniBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnBuscarPaciente, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jpPacienteLayout.createSequentialGroup()
                        .addGroup(jpPacienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblPaciente)
                            .addComponent(lblPacienteSeleccionado))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jpPacienteLayout.setVerticalGroup(
            jpPacienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpPacienteLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblPaciente)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jpPacienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblDniPaciente)
                    .addComponent(txtPacienteDniBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBuscarPaciente))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblPacienteSeleccionado)
                .addContainerGap(33, Short.MAX_VALUE))
        );

        jLabel1.setText("Datos de la Cita:");

        jLabel2.setText("Especialidad:");

        jLabel3.setText("Medico:");

        jLabel4.setText("Proximas Fechas:");

        jLabel5.setText("Fecha (Manual)");

        jLabel6.setText("Hora:");

        jLabel7.setText("Motivo:");

        cmbEspecialidadFiltro.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbEspecialidadFiltroActionPerformed(evt);
            }
        });

        cmbMedico.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbMedicoActionPerformed(evt);
            }
        });

        cmbProximasFechas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbProximasFechasActionPerformed(evt);
            }
        });

        txtFechaCita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtFechaCitaActionPerformed(evt);
            }
        });

        txtMotivoCita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtMotivoCitaActionPerformed(evt);
            }
        });

        btnAgendarCita.setText("Agendar Cita");
        btnAgendarCita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgendarCitaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnAgendarCita, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(cmbEspecialidadFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(cmbMedico, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(cmbProximasFechas, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addComponent(jLabel1)
                            .addComponent(jLabel5)
                            .addComponent(jLabel7))
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(120, 120, 120)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(cmbHoraCita, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtFechaCita, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(txtMotivoCita, javax.swing.GroupLayout.PREFERRED_SIZE, 232, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(cmbEspecialidadFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(cmbMedico, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(cmbProximasFechas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(txtFechaCita, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(cmbHoraCita, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(txtMotivoCita, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 12, Short.MAX_VALUE)
                .addComponent(btnAgendarCita))
        );

        tblCitas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "ID Cita", "Fecha", "Hora", "Paciente", "Medico", "Motivo", "Estado"
            }
        ));
        tblCitas.setMaximumSize(new java.awt.Dimension(950, 250));
        tblCitas.setMinimumSize(new java.awt.Dimension(950, 250));
        tblCitas.setPreferredSize(new java.awt.Dimension(950, 250));
        jScrollPane1.setViewportView(tblCitas);

        btnCancelarCita.setText("Cancelar Cita Seleccionada");
        btnCancelarCita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarCitaActionPerformed(evt);
            }
        });

        btnMarcarAtendida.setText("Marcar Atendida Seleccionada");
        btnMarcarAtendida.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMarcarAtendidaActionPerformed(evt);
            }
        });

        btnRefrescarCitas.setText("Refrescar Citas");
        btnRefrescarCitas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefrescarCitasActionPerformed(evt);
            }
        });

        btnMarcarNoAsistida.setText("Marcar No Asistida Seleccionada");
        btnMarcarNoAsistida.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMarcarNoAsistidaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 950, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnCancelarCita, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(btnMarcarAtendida, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(34, 34, 34)
                                .addComponent(btnMarcarNoAsistida, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(btnRefrescarCitas, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jpPaciente, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                .addGap(25, 25, 25))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addComponent(jpPaciente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 251, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCancelarCita)
                    .addComponent(btnMarcarAtendida)
                    .addComponent(btnRefrescarCitas)
                    .addComponent(btnMarcarNoAsistida))
                .addGap(39, 39, 39))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtMotivoCitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtMotivoCitaActionPerformed

    }//GEN-LAST:event_txtMotivoCitaActionPerformed

    private void btnMarcarAtendidaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMarcarAtendidaActionPerformed
        cambiarEstadoCitaSeleccionada("Atendida");
    }//GEN-LAST:event_btnMarcarAtendidaActionPerformed

    private void btnRefrescarCitasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefrescarCitasActionPerformed
        cargarCitasEnTabla(null, null);
    }//GEN-LAST:event_btnRefrescarCitasActionPerformed

    private void btnMarcarNoAsistidaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMarcarNoAsistidaActionPerformed
        cambiarEstadoCitaSeleccionada("No Asistida");
    }//GEN-LAST:event_btnMarcarNoAsistidaActionPerformed

    private void btnBuscarPacienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuscarPacienteActionPerformed
        buscarPaciente();
    }//GEN-LAST:event_btnBuscarPacienteActionPerformed

    private void cmbEspecialidadFiltroActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbEspecialidadFiltroActionPerformed
        cargarMedicosPorEspecialidad();
    }//GEN-LAST:event_cmbEspecialidadFiltroActionPerformed

    private void cmbMedicoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbMedicoActionPerformed
        cargarProximasFechasDisponibles();
    }//GEN-LAST:event_cmbMedicoActionPerformed

    private void txtFechaCitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtFechaCitaActionPerformed
        cargarHorasDisponibles();
    }//GEN-LAST:event_txtFechaCitaActionPerformed

    private void btnAgendarCitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgendarCitaActionPerformed
        agendarNuevaCita();
    }//GEN-LAST:event_btnAgendarCitaActionPerformed

    private void btnCancelarCitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarCitaActionPerformed
        cambiarEstadoCitaSeleccionada("Cancelada");
    }//GEN-LAST:event_btnCancelarCitaActionPerformed

    private void cmbProximasFechasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbProximasFechasActionPerformed

        if (cmbProximasFechas.getSelectedIndex() > 0) {
            txtFechaCita.setText((String) cmbProximasFechas.getSelectedItem());
            cargarHorasDisponibles();
        } else {
            txtFechaCita.setText("");
            cmbHoraCita.removeAllItems();
            cmbHoraCita.addItem("Seleccionar Hora");
        }

    }//GEN-LAST:event_cmbProximasFechasActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(GestionCitasForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GestionCitasForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GestionCitasForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GestionCitasForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new GestionCitasForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAgendarCita;
    private javax.swing.JButton btnBuscarPaciente;
    private javax.swing.JButton btnCancelarCita;
    private javax.swing.JButton btnMarcarAtendida;
    private javax.swing.JButton btnMarcarNoAsistida;
    private javax.swing.JButton btnRefrescarCitas;
    private javax.swing.JComboBox<String> cmbEspecialidadFiltro;
    private javax.swing.JComboBox<String> cmbHoraCita;
    private javax.swing.JComboBox<String> cmbMedico;
    private javax.swing.JComboBox<String> cmbProximasFechas;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel jpPaciente;
    private javax.swing.JLabel lblDniPaciente;
    private javax.swing.JLabel lblPaciente;
    private javax.swing.JLabel lblPacienteSeleccionado;
    private javax.swing.JTable tblCitas;
    private javax.swing.JTextField txtFechaCita;
    private javax.swing.JTextField txtMotivoCita;
    private javax.swing.JTextField txtPacienteDniBusqueda;
    // End of variables declaration//GEN-END:variables
}
