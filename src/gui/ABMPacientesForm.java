package gui;

import conexion.ConexionSQLServer;
import modelo.Paciente;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZoneId;
import java.util.Date;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.Image;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class ABMPacientesForm extends javax.swing.JFrame {

    private ConexionSQLServer conexion;
    private DefaultTableModel tablaPacientesModel;

    public ABMPacientesForm() {
        initComponents();
    }

    public ABMPacientesForm(ConexionSQLServer conexion) {
        this.conexion = conexion;
        initComponents();
        setTitle("ABM Pacientes");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(null);
        try {
            Image icon = ImageIO.read(getClass().getResource("/resources/logo.png"));
            this.setIconImage(icon);
        } catch (IOException e) {
            System.err.println("ERROR: No se pudo cargar el logo: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: La ruta del logo es invalida o el archivo no existe: " + e.getMessage());
        }

        inicializarModeloTabla();
        
        tblPacientes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblPacientes.getSelectedRow() != -1) {
                mostrarPacienteSeleccionado();
            }
        });
        
        setLocationRelativeTo(null);
        cargarPacientesEnTabla();
    }
    
    private void inicializarModeloTabla() {
        tablaPacientesModel = new DefaultTableModel(new Object[]{"ID", "DNI", "Nombre", "Apellido", "Telefono", "Correo"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
      
        tblPacientes.setModel(tablaPacientesModel);
    }

    private void limpiarCampos() {
        txtIdPaciente.setText("");
        txtDni.setText("");
        txtNombre.setText("");
        txtApellido.setText("");
        txtFechaNacimiento.setText("");
        txtTelefono.setText("");
        txtCorreoElectronico.setText("");
        txtDireccion.setText("");
        tblPacientes.clearSelection();
    }

    private void guardarPaciente() {
        try {
            int dni = Integer.parseInt(txtDni.getText());
            String nombre = txtNombre.getText();
            String apellido = txtApellido.getText();
            LocalDate fechaNacimiento = null;
            if (!txtFechaNacimiento.getText().isEmpty()) {
                try {
                    fechaNacimiento = LocalDate.parse(txtFechaNacimiento.getText());
                } catch (DateTimeParseException ex) {
                    JOptionPane.showMessageDialog(this, "Formato de fecha de nacimiento invalido. Use AAAA-MM-DD.", "Error de Fecha", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            String telefono = txtTelefono.getText();
            String correo = txtCorreoElectronico.getText();
            String direccion = txtDireccion.getText();

            if (nombre.isEmpty() || apellido.isEmpty() || dni <= 0) {
                JOptionPane.showMessageDialog(this, "Nombre, apellido y DNI son obligatorios.", "Datos Faltantes", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Paciente paciente = new Paciente(dni, nombre, apellido, fechaNacimiento != null ? Date.from(fechaNacimiento.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null, telefono, correo, direccion);

            Connection conn = null;
            try {
                conn = conexion.getConexion();
                if (conn == null) {
                    throw new SQLException("No hay conexion a la base de datos.");
                }
                conn.setAutoCommit(false);

                if (txtIdPaciente.getText().isEmpty()) {
                    if (pacienteExisteEnBD(dni)) { // Este metodo ahora chequea Pacientes por DNI
                        JOptionPane.showMessageDialog(this, "Ya existe un paciente con el DNI " + dni + ".", "DNI Duplicado", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    String insertSQL = "INSERT INTO Pacientes (Dni, Nombre, Apellido, FechaNacimiento, Telefono, CorreoElectronico, Direccion) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
                        pstmt.setInt(1, paciente.getDni());
                        pstmt.setString(2, paciente.getNombre());
                        pstmt.setString(3, paciente.getApellido());
                        pstmt.setDate(4, paciente.getFechaNacimiento() != null ? new java.sql.Date(paciente.getFechaNacimiento().getTime()) : null);
                        pstmt.setString(5, paciente.getTelefono());
                        pstmt.setString(6, paciente.getCorreoElectronico());
                        pstmt.setString(7, paciente.getDireccion());
                        conexion.ejecutarUpdate(pstmt);
                        try (ResultSet rs = pstmt.getGeneratedKeys()) {
                            if (rs.next()) {
                                paciente.setIdPaciente(rs.getInt(1));
                            }
                        }
                    }
                    JOptionPane.showMessageDialog(this, "Paciente registrado con exito.", "Exito", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    paciente.setIdPaciente(Integer.parseInt(txtIdPaciente.getText()));
                    String updateSQL = "UPDATE Pacientes SET Dni = ?, Nombre = ?, Apellido = ?, FechaNacimiento = ?, Telefono = ?, CorreoElectronico = ?, Direccion = ? WHERE IdPaciente = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
                        pstmt.setInt(1, paciente.getDni());
                        pstmt.setString(2, paciente.getNombre());
                        pstmt.setString(3, paciente.getApellido());
                        pstmt.setDate(4, paciente.getFechaNacimiento() != null ? new java.sql.Date(paciente.getFechaNacimiento().getTime()) : null);
                        pstmt.setString(5, paciente.getTelefono());
                        pstmt.setString(6, paciente.getCorreoElectronico());
                        pstmt.setString(7, paciente.getDireccion());
                        pstmt.setInt(8, paciente.getIdPaciente());
                        conexion.ejecutarUpdate(pstmt);
                    }
                    JOptionPane.showMessageDialog(this, "Paciente actualizado con exito.", "Exito", JOptionPane.INFORMATION_MESSAGE);
                }
                conn.commit();
                cargarPacientesEnTabla();
                limpiarCampos();
            } catch (SQLException e) {
                System.err.println("ERROR: Fallo al guardar paciente en BD: " + e.getMessage());
                try {
                    if (conn != null && !conn.isClosed()) {
                        try {
                            conn.rollback();
                        } catch (SQLException ex) {
                            System.err.println("ERROR: Fallo en rollback: " + ex.getMessage());
                        }
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(ABMPacientesForm.class.getName()).log(Level.SEVERE, null, ex);
                }
                JOptionPane.showMessageDialog(this, "Error al guardar paciente: " + e.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            } finally {
                try {
                    if (conn != null && !conn.isClosed()) {
                        try {
                            conn.setAutoCommit(true);
                        } catch (SQLException ex) {
                            System.err.println("ERROR: Fallo al restaurar auto-commit: " + ex.getMessage());
                        }
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(ABMPacientesForm.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "DNI debe ser un numero valido.", "Error de DNI", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarPaciente() {
        int selectedRow = tblPacientes.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un paciente de la tabla para eliminar.", "Sin Seleccion", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int idPaciente = (Integer) tablaPacientesModel.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this, "¿Esta seguro de eliminar este paciente? Esta accion no se puede deshacer.", "Confirmar Eliminacion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            Connection conn = null;
            try {
                conn = conexion.getConexion();
                if (conn == null) {
                    throw new SQLException("No hay conexion a la base de datos.");
                }
                conn.setAutoCommit(false);

                String deleteCitasSQL = "DELETE FROM Citas WHERE IdPaciente = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteCitasSQL)) {
                    pstmt.setInt(1, idPaciente);
                    pstmt.executeUpdate();
                }
                String deleteHistoriasSQL = "DELETE FROM HistoriasClinicas WHERE IdPaciente = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteHistoriasSQL)) {
                    pstmt.setInt(1, idPaciente);
                    pstmt.executeUpdate();
                }

                String deletePacienteSQL = "DELETE FROM Pacientes WHERE IdPaciente = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deletePacienteSQL)) {
                    pstmt.setInt(1, idPaciente);
                    pstmt.executeUpdate();
                }
                conn.commit();
                JOptionPane.showMessageDialog(this, "Paciente eliminado con exito.", "Exito", JOptionPane.INFORMATION_MESSAGE);
                cargarPacientesEnTabla();
                limpiarCampos();
            } catch (SQLException e) {
                System.err.println("ERROR: Fallo al eliminar paciente en BD: " + e.getMessage());
                try {
                    if (conn != null && !conn.isClosed()) {
                        try {
                            conn.rollback();
                        } catch (SQLException ex) {
                            System.err.println("ERROR: Fallo en rollback: " + ex.getMessage());
                        }
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(ABMPacientesForm.class.getName()).log(Level.SEVERE, null, ex);
                }
                JOptionPane.showMessageDialog(this, "Error al eliminar paciente: " + e.getMessage() + "\nAsegurese que no tenga citas o historias clinicas pendientes.", "Error BD", JOptionPane.ERROR_MESSAGE);
            } finally {
                try {
                    if (conn != null && !conn.isClosed()) {
                        try {
                            conn.setAutoCommit(true);
                        } catch (SQLException ex) {
                            System.err.println("ERROR: Fallo al restaurar auto-commit: " + ex.getMessage());
                        }
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(ABMPacientesForm.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void buscarPacientePorDni() {
        try {
            int dni = Integer.parseInt(txtDni.getText());
            String query = "SELECT IdPaciente, Dni, Nombre, Apellido, FechaNacimiento, Telefono, CorreoElectronico, Direccion FROM Pacientes WHERE Dni = ?";
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                conn = conexion.getConexion();
                if (conn == null) {
                    throw new SQLException("No hay conexion a la base de datos.");
                }
                pstmt = conn.prepareStatement(query);

                pstmt.setInt(1, dni); // <<-- AQUI LA CORRECCION: Setea el parametro 1

                rs = conexion.ejecutarQuery(pstmt);
                if (rs.next()) {
                    txtIdPaciente.setText(String.valueOf(rs.getInt("IdPaciente")));
                    txtDni.setText(String.valueOf(rs.getInt("Dni")));
                    txtNombre.setText(rs.getString("Nombre"));
                    txtApellido.setText(rs.getString("Apellido"));
                    if (rs.getDate("FechaNacimiento") != null) {
                        txtFechaNacimiento.setText(rs.getDate("FechaNacimiento").toLocalDate().toString());
                    } else {
                        txtFechaNacimiento.setText("");
                    }
                    txtTelefono.setText(rs.getString("Telefono"));
                    txtCorreoElectronico.setText(rs.getString("CorreoElectronico"));
                    txtDireccion.setText(rs.getString("Direccion"));
                    JOptionPane.showMessageDialog(this, "Paciente encontrado.", "Busqueda Exitosa", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Paciente no encontrado con DNI: " + dni, "No Encontrado", JOptionPane.WARNING_MESSAGE);
                    limpiarCampos();
                    txtDni.setText(String.valueOf(dni));
                }
            } catch (SQLException e) {
                System.err.println("ERROR: Fallo al buscar paciente por DNI: " + e.getMessage());
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

    private void cargarPacientesEnTabla() {
        tablaPacientesModel.setRowCount(0);
        String query = "SELECT IdPaciente, Dni, Nombre, Apellido, Telefono, CorreoElectronico FROM Pacientes ORDER BY Apellido, Nombre";
        try (Connection conn = conexion.getConexion(); Statement stm = conn.createStatement(); ResultSet rs = stm.executeQuery(query)) {
            while (rs.next()) {
                tablaPacientesModel.addRow(new Object[]{
                    rs.getInt("IdPaciente"),
                    rs.getInt("Dni"),
                    rs.getString("Nombre"),
                    rs.getString("Apellido"),
                    rs.getString("Telefono"),
                    rs.getString("CorreoElectronico")
                });
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al cargar pacientes en tabla: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error al cargar pacientes: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarPacienteSeleccionado() {
        int selectedRow = tblPacientes.getSelectedRow();
        if (selectedRow != -1) {
            int idPaciente = (Integer) tablaPacientesModel.getValueAt(selectedRow, 0);
            String query = "SELECT IdPaciente, Dni, Nombre, Apellido, FechaNacimiento, Telefono, CorreoElectronico, Direccion FROM Pacientes WHERE IdPaciente = ?";
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                conn = conexion.getConexion();
                if (conn == null) {
                    throw new SQLException("No hay conexion a la base de datos.");
                }
                pstmt = conn.prepareStatement(query);

                pstmt.setInt(1, idPaciente);
                rs = conexion.ejecutarQuery(pstmt);
                if (rs.next()) {
                    txtIdPaciente.setText(String.valueOf(rs.getInt("IdPaciente")));
                    txtDni.setText(String.valueOf(rs.getInt("Dni")));
                    txtNombre.setText(rs.getString("Nombre"));
                    txtApellido.setText(rs.getString("Apellido"));
                    if (rs.getDate("FechaNacimiento") != null) {
                        txtFechaNacimiento.setText(rs.getDate("FechaNacimiento").toLocalDate().toString());
                    } else {
                        txtFechaNacimiento.setText("");
                    }
                    txtTelefono.setText(rs.getString("Telefono"));
                    txtCorreoElectronico.setText(rs.getString("CorreoElectronico"));
                    txtDireccion.setText(rs.getString("Direccion"));
                }
            } catch (SQLException e) {
                System.err.println("ERROR: Fallo al mostrar paciente seleccionado: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error al cargar detalles del paciente: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
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
    }

    private boolean pacienteExisteEnBD(int dni) {
        String query = "SELECT COUNT(*) FROM Pacientes WHERE Dni = ?";
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
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar DNI duplicado: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error de BD al verificar DNI.", "Error DB", JOptionPane.ERROR_MESSAGE);
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

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        txtDni = new javax.swing.JTextField();
        txtIdPaciente = new javax.swing.JTextField();
        txtNombre = new javax.swing.JTextField();
        txtApellido = new javax.swing.JTextField();
        txtFechaNacimiento = new javax.swing.JTextField();
        txtTelefono = new javax.swing.JTextField();
        txtCorreoElectronico = new javax.swing.JTextField();
        txtDireccion = new javax.swing.JTextField();
        btnNuevo = new javax.swing.JButton();
        btnGuardar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        btnBuscar = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblPacientes = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(900, 600));
        setResizable(false);

        jLabel1.setText("ID Paciente:");

        jLabel2.setText("DNI:");

        jLabel3.setText("Nombre:");

        jLabel4.setText("Apellido:");

        jLabel5.setText("Fecha Nac. (AAAA-MM-DD):");

        jLabel6.setText("Telefono:");

        jLabel7.setText("Correo:");

        jLabel8.setText("Direccion:");

        txtIdPaciente.setEditable(false);
        txtIdPaciente.setEnabled(false);

        btnNuevo.setText("Nuevo");
        btnNuevo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNuevoActionPerformed(evt);
            }
        });

        btnGuardar.setText("Guardar");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });

        btnEliminar.setText("Eliminar");
        btnEliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarActionPerformed(evt);
            }
        });

        btnBuscar.setText("Buscar (Por DNI)");
        btnBuscar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBuscarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(29, 29, 29)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(txtDni, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtIdPaciente, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtApellido, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtFechaNacimiento, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtTelefono, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtCorreoElectronico, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtDireccion, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnNuevo, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(0, 6, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(txtIdPaciente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(txtDni, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(21, 21, 21)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(txtApellido, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(txtFechaNacimiento, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(22, 22, 22)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(txtTelefono, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(txtCorreoElectronico, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(20, 20, 20)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel8)
                            .addComponent(txtDireccion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btnNuevo, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))))
        );

        tblPacientes.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        tblPacientes.setToolTipText("");
        jScrollPane1.setViewportView(tblPacientes);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 808, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 12, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 30, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 376, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(23, 23, 23))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        guardarPaciente();
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnBuscarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuscarActionPerformed
        buscarPacientePorDni();
    }//GEN-LAST:event_btnBuscarActionPerformed

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        eliminarPaciente();
    }//GEN-LAST:event_btnEliminarActionPerformed

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevoActionPerformed
        limpiarCampos();
    }//GEN-LAST:event_btnNuevoActionPerformed

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
            java.util.logging.Logger.getLogger(ABMPacientesForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ABMPacientesForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ABMPacientesForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ABMPacientesForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ABMPacientesForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tblPacientes;
    private javax.swing.JTextField txtApellido;
    private javax.swing.JTextField txtCorreoElectronico;
    private javax.swing.JTextField txtDireccion;
    private javax.swing.JTextField txtDni;
    private javax.swing.JTextField txtFechaNacimiento;
    private javax.swing.JTextField txtIdPaciente;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtTelefono;
    // End of variables declaration//GEN-END:variables
}
