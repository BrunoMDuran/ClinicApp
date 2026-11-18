package gui;

import conexion.ConexionSQLServer;
import modelo.Usuario;
import modelo.Medico;
import modelo.Especialidad;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

public class ABMUsuariosForm extends javax.swing.JFrame {

    private ConexionSQLServer conexion;
    private List<Medico> medicosDisponibles;
    private DefaultTableModel tablaUsuariosModel;

    public ABMUsuariosForm() {
        initComponents();        
        cargarUsuariosEnTabla();
    }

    public ABMUsuariosForm(ConexionSQLServer conexion) {
        this.conexion = conexion;
        this.medicosDisponibles = new ArrayList<>();
        initComponents();
        cmbMedicoAsociado.setEnabled(false);
        setTitle("ABM Usuarios");
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
        setLocationRelativeTo(null);
        cargarDatosIniciales();
        cargarUsuariosEnTabla();
    }

    private void actualizarEstadoMedicoAsociado() {

        String rolSeleccionado = (String) cmbRol.getSelectedItem();

        switch (rolSeleccionado) {
            case "Administrador":
                cmbMedicoAsociado.setEnabled(false);
                cmbMedicoAsociado.setSelectedItem("Ninguno");
                break;
            case "Secretario_Admin":
                cmbMedicoAsociado.setEnabled(false);
                cmbMedicoAsociado.setSelectedItem("Ninguno");
                break;
            default:
                // Si es cualquier otro rol, lo habilitamos
                cmbMedicoAsociado.setEnabled(true);
                break;
        }
    }

    private void inicializarModeloTabla() {
        tablaUsuariosModel = new DefaultTableModel(new Object[]{"ID", "Usuario", "Rol", "Medico Asociado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblUsuarios.setModel(tablaUsuariosModel);
        tblUsuarios.getTableHeader().setResizingAllowed(false);

        javax.swing.table.TableColumnModel columnModel = tblUsuarios.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(40);
        columnModel.getColumn(1).setPreferredWidth(100);
        columnModel.getColumn(2).setPreferredWidth(150);
        columnModel.getColumn(3).setPreferredWidth(220);

        tblUsuarios.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblUsuarios.getSelectedRow() != -1) {
                mostrarUsuarioSeleccionado();
            }
        });
    }

    private void cargarDatosIniciales() {
        cmbMedicoAsociado.addItem("Ninguno");
        String queryMedicos = "SELECT m.IdMedico, m.Nombre, m.Apellido, m.NumeroMatricula, m.Telefono, e.IdEspecialidad, e.NombreEspecialidad FROM Medicos m JOIN Especialidades e ON m.IdEspecialidad = e.IdEspecialidad ORDER BY m.Apellido, m.Nombre";
        Connection conn = null;
        Statement stm = null;
        ResultSet rs = null;
        try {
            conn = conexion.getConexion();
            if (conn == null) {
                throw new SQLException("No hay conexion a la base de datos.");
            }
            stm = conn.createStatement();
            rs = stm.executeQuery(queryMedicos);
            while (rs.next()) {
                Especialidad especialidad = new Especialidad(rs.getInt("IdEspecialidad"), rs.getString("NombreEspecialidad"));
                Medico m = new Medico(rs.getInt("IdMedico"), rs.getString("Nombre"), rs.getString("Apellido"), rs.getString("NumeroMatricula"), especialidad, rs.getString("Telefono"));
                medicosDisponibles.add(m);
                cmbMedicoAsociado.addItem(m.getNombre() + " " + m.getApellido());
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al cargar medicos para ABM Usuarios: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error al cargar medicos.", "Error DB", JOptionPane.ERROR_MESSAGE);
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
    }

    private void limpiarCampos() {
        actualizarEstadoMedicoAsociado();
        txtIdUsuario.setText("");
        txtNombreUsuario.setText("");
        txtContrasena.setText("");
        cmbRol.setSelectedIndex(0);
        cmbMedicoAsociado.setSelectedIndex(0);        
        tblUsuarios.clearSelection();
    }

    private void guardarUsuario() {
        String nombreUsuario = txtNombreUsuario.getText();
        String contrasena = txtContrasena.getText();
        String rol = (String) cmbRol.getSelectedItem();
        Integer idMedicoAsociado = null;

        if (nombreUsuario.isEmpty() || contrasena.isEmpty() || rol == null) {
            JOptionPane.showMessageDialog(this, "Usuario, contrasena y rol son obligatorios.", "Datos Faltantes", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if ("Medico".equals(rol)) {
            int selectedMedicoIndex = cmbMedicoAsociado.getSelectedIndex();
            if (selectedMedicoIndex <= 0) {
                JOptionPane.showMessageDialog(this, "Seleccione el medico asociado para el rol Medico.", "Medico No Seleccionado", JOptionPane.WARNING_MESSAGE);
                return;
            }
            idMedicoAsociado = medicosDisponibles.get(selectedMedicoIndex - 1).getIdMedico();
        }

        Usuario usuario = new Usuario(nombreUsuario, contrasena, rol, idMedicoAsociado);

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rsGeneratedKeys = null;
        try {
            conn = conexion.getConexion();
            if (conn == null) {
                throw new SQLException("No hay conexion a la base de datos.");
            }
            conn.setAutoCommit(false);

            if (txtIdUsuario.getText().isEmpty()) {
                if (usuarioExisteEnBD(nombreUsuario)) {
                    JOptionPane.showMessageDialog(this, "El nombre de usuario '" + nombreUsuario + "' ya existe.", "Usuario Duplicado", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String insertSQL = "INSERT INTO Usuarios (NombreUsuario, ContrasenaHash, Rol, IdMedicoAsociado) VALUES (?, ?, ?, ?)";
                pstmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);
                pstmt.setString(1, usuario.getNombreUsuario());
                pstmt.setString(2, usuario.getContrasenaHash());
                pstmt.setString(3, usuario.getRol());
                if (usuario.getIdMedicoAsociado() != null) {
                    pstmt.setInt(4, usuario.getIdMedicoAsociado());
                } else {
                    pstmt.setNull(4, java.sql.Types.INTEGER);
                }
                conexion.ejecutarUpdate(pstmt);
                rsGeneratedKeys = pstmt.getGeneratedKeys();
                if (rsGeneratedKeys.next()) {
                    usuario.setIdUsuario(rsGeneratedKeys.getInt(1));
                }
                JOptionPane.showMessageDialog(this, "Usuario registrado con exito.", "Exito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                usuario.setIdUsuario(Integer.parseInt(txtIdUsuario.getText()));
                String updateSQL = "UPDATE Usuarios SET NombreUsuario = ?, ContrasenaHash = ?, Rol = ?, IdMedicoAsociado = ? WHERE IdUsuario = ?";
                pstmt = conn.prepareStatement(updateSQL);
                pstmt.setString(1, usuario.getNombreUsuario());
                pstmt.setString(2, usuario.getContrasenaHash());
                pstmt.setString(3, usuario.getRol());
                if (usuario.getIdMedicoAsociado() != null) {
                    pstmt.setInt(4, usuario.getIdMedicoAsociado());
                } else {
                    pstmt.setNull(4, java.sql.Types.INTEGER);
                }
                pstmt.setInt(5, usuario.getIdUsuario());
                conexion.ejecutarUpdate(pstmt);
                JOptionPane.showMessageDialog(this, "Usuario actualizado con exito.", "Exito", JOptionPane.INFORMATION_MESSAGE);
            }
            conn.commit();
            cargarUsuariosEnTabla();
            limpiarCampos();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "ID de usuario invalido.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al guardar usuario en BD: " + e.getMessage());
            try {
                if (conn != null && !conn.isClosed()) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        System.err.println("ERROR: Fallo en rollback: " + ex.getMessage());
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(ABMUsuariosForm.class.getName()).log(Level.SEVERE, null, ex);
            }
            JOptionPane.showMessageDialog(this, "Error al guardar usuario: " + e.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
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
                Logger.getLogger(ABMUsuariosForm.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void eliminarUsuario() {
        int selectedRow = tblUsuarios.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario de la tabla para eliminar.", "Sin Seleccion", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int idUsuario = (Integer) tablaUsuariosModel.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this, "¿Esta seguro de eliminar este usuario?", "Confirmar Eliminacion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            Connection conn = null;
            PreparedStatement pstmt = null;
            try {
                conn = conexion.getConexion();
                if (conn == null) {
                    throw new SQLException("No hay conexion a la base de datos.");
                }
                conn.setAutoCommit(false);

                String deleteSQL = "DELETE FROM Usuarios WHERE IdUsuario = ?";
                pstmt = conn.prepareStatement(deleteSQL);
                pstmt.setInt(1, idUsuario);
                conexion.ejecutarUpdate(pstmt);
                conn.commit();
                JOptionPane.showMessageDialog(this, "Usuario eliminado con exito.", "Exito", JOptionPane.INFORMATION_MESSAGE);
                cargarUsuariosEnTabla();
                limpiarCampos();
            } catch (SQLException e) {
                System.err.println("ERROR: Fallo al eliminar usuario en BD: " + e.getMessage());
                try {
                    if (conn != null && !conn.isClosed()) {
                        try {
                            conn.rollback();
                        } catch (SQLException ex) {
                            System.err.println("ERROR: Fallo en rollback: " + ex.getMessage());
                        }
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(ABMUsuariosForm.class.getName()).log(Level.SEVERE, null, ex);
                }
                JOptionPane.showMessageDialog(this, "Error al eliminar usuario: " + e.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
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
                    Logger.getLogger(ABMUsuariosForm.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void cargarUsuariosEnTabla() {
        tablaUsuariosModel.setRowCount(0);
        String query = "SELECT u.IdUsuario, u.NombreUsuario, u.Rol, m.Nombre AS MedicoNombre, m.Apellido AS MedicoApellido "
                + "FROM Usuarios u LEFT JOIN Medicos m ON u.IdMedicoAsociado = m.IdMedico ORDER BY u.NombreUsuario";
        Connection conn = null;
        Statement stm = null;
        ResultSet rs = null;
        try {
            conn = conexion.getConexion();
            if (conn == null) {
                throw new SQLException("No hay conexion a la base de datos.");
            }
            stm = conn.createStatement();
            rs = stm.executeQuery(query);
            while (rs.next()) {
                String medicoAsociado = rs.getString("MedicoNombre");
                if (medicoAsociado != null) {
                    medicoAsociado += " " + rs.getString("MedicoApellido");
                } else {
                    medicoAsociado = "N/A";
                }
                tablaUsuariosModel.addRow(new Object[]{
                    rs.getInt("IdUsuario"),
                    rs.getString("NombreUsuario"),
                    rs.getString("Rol"),
                    medicoAsociado
                });
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al cargar usuarios en tabla: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error al cargar usuarios: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
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
    }

    private void mostrarUsuarioSeleccionado() {
        int selectedRow = tblUsuarios.getSelectedRow();
        if (selectedRow != -1) {
            int idUsuario = (Integer) tablaUsuariosModel.getValueAt(selectedRow, 0);
            String query = "SELECT IdUsuario, NombreUsuario, ContrasenaHash, Rol, IdMedicoAsociado FROM Usuarios WHERE IdUsuario = ?";
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;

            try {
                conn = conexion.getConexion();
                if (conn == null) {
                    throw new SQLException("No hay conexion a la base de datos.");
                }
                pstmt = conn.prepareStatement(query);

                pstmt.setInt(1, idUsuario);
                rs = conexion.ejecutarQuery(pstmt);

                if (rs.next()) {
                    txtIdUsuario.setText(String.valueOf(rs.getInt("IdUsuario")));
                    txtNombreUsuario.setText(rs.getString("NombreUsuario"));
                    txtContrasena.setText(rs.getString("ContrasenaHash"));
                    cmbRol.setSelectedItem(rs.getString("Rol"));

                    Integer idMedicoAsociado = rs.getObject("IdMedicoAsociado") != null ? rs.getInt("IdMedicoAsociado") : null;
                    if (idMedicoAsociado != null) {
                        for (int i = 0; i < medicosDisponibles.size(); i++) {
                            if (medicosDisponibles.get(i).getIdMedico() == idMedicoAsociado) {
                                cmbMedicoAsociado.setSelectedIndex(i + 1);
                                break;
                            }
                        }
                    } else {
                        cmbMedicoAsociado.setSelectedIndex(0);
                    }
                    cmbMedicoAsociado.setEnabled("Medico".equals(cmbRol.getSelectedItem()));
                }
            } catch (SQLException e) {
                System.err.println("ERROR: Fallo al mostrar usuario seleccionado: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error al cargar detalles del usuario: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
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

    private boolean usuarioExisteEnBD(String nombreUsuario) {
        String query = "SELECT COUNT(*) FROM Usuarios WHERE NombreUsuario = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = conexion.getConexion();
            if (conn == null) {
                throw new SQLException("No hay conexion a la base de datos.");
            }
            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, nombreUsuario);
            rs = conexion.ejecutarQuery(pstmt);
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar usuario duplicado: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error de BD al verificar usuario.", "Error DB", JOptionPane.ERROR_MESSAGE);
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
        txtIdUsuario = new javax.swing.JTextField();
        txtNombreUsuario = new javax.swing.JTextField();
        txtContrasena = new javax.swing.JTextField();
        cmbRol = new javax.swing.JComboBox<>();
        cmbMedicoAsociado = new javax.swing.JComboBox<>();
        btnNuevo = new javax.swing.JButton();
        btnGuardar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblUsuarios = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        jLabel1.setText("ID Usuario:");

        jLabel2.setText("Usuario:");

        jLabel3.setText("Rol:");

        jLabel4.setText("Contrasena:");

        jLabel5.setText("Medico Asociado:");

        txtIdUsuario.setEditable(false);
        txtIdUsuario.setEnabled(false);

        cmbRol.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Administrador", "Secretario_Admin", "Medico" }));
        cmbRol.setName(""); // NOI18N
        cmbRol.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cmbRolItemStateChanged(evt);
            }
        });
        cmbRol.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbRolActionPerformed(evt);
            }
        });

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

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel4)
                    .addComponent(jLabel3)
                    .addComponent(jLabel5))
                .addGap(77, 77, 77)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtNombreUsuario)
                    .addComponent(cmbRol, 0, 140, Short.MAX_VALUE)
                    .addComponent(cmbMedicoAsociado, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtContrasena)
                    .addComponent(txtIdUsuario))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 160, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnNuevo, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnGuardar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEliminar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(txtIdUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(txtNombreUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(txtContrasena, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(cmbRol, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(cmbMedicoAsociado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btnNuevo, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(13, Short.MAX_VALUE))
        );

        jScrollPane2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jScrollPane2MouseClicked(evt);
            }
        });

        tblUsuarios.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "ID", "Usuario", "Rol", "Medico Asociado"
            }
        ));
        tblUsuarios.setName(""); // NOI18N
        jScrollPane2.setViewportView(tblUsuarios);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane2))
                .addContainerGap(16, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 331, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(24, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        eliminarUsuario();
    }//GEN-LAST:event_btnEliminarActionPerformed

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevoActionPerformed
        limpiarCampos();
    }//GEN-LAST:event_btnNuevoActionPerformed

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        guardarUsuario();
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void jScrollPane2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jScrollPane2MouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_jScrollPane2MouseClicked

    private void cmbRolActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbRolActionPerformed
        
    }//GEN-LAST:event_cmbRolActionPerformed

    private void cmbRolItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cmbRolItemStateChanged
        if (evt.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
            actualizarEstadoMedicoAsociado();
        }
    }//GEN-LAST:event_cmbRolItemStateChanged

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
            java.util.logging.Logger.getLogger(ABMUsuariosForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ABMUsuariosForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ABMUsuariosForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ABMUsuariosForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ABMUsuariosForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JComboBox<String> cmbMedicoAsociado;
    private javax.swing.JComboBox<String> cmbRol;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable tblUsuarios;
    private javax.swing.JTextField txtContrasena;
    private javax.swing.JTextField txtIdUsuario;
    private javax.swing.JTextField txtNombreUsuario;
    // End of variables declaration//GEN-END:variables
}
