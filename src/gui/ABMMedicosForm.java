package gui;

import conexion.ConexionSQLServer;
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

public class ABMMedicosForm extends javax.swing.JFrame {

    private ConexionSQLServer conexion;
    private List<Especialidad> especialidadesDisponibles;
    private DefaultTableModel tablaMedicosModel;

    public ABMMedicosForm() {
        initComponents();
    }

    public ABMMedicosForm(ConexionSQLServer conexion) {
        this.conexion = conexion;
        this.especialidadesDisponibles = new ArrayList<>();
        initComponents();
        setTitle("ABM Medicos");
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
        cargarMedicosEnTabla();
    }

    private void inicializarModeloTabla() {
        tablaMedicosModel = new DefaultTableModel(
                new Object[]{"ID", "Nombre", "Apellido", "Matricula", "Especialidad", "Telefono"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblMedicos.setModel(tablaMedicosModel);

        javax.swing.table.TableColumnModel columnModel = tblMedicos.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(60);
        columnModel.getColumn(1).setPreferredWidth(140);
        columnModel.getColumn(2).setPreferredWidth(140);
        columnModel.getColumn(3).setPreferredWidth(100);
        columnModel.getColumn(4).setPreferredWidth(200);
        columnModel.getColumn(5).setPreferredWidth(110);

        tblMedicos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblMedicos.getSelectedRow() != -1) {
                mostrarMedicoSeleccionado();
            }
        });
    }

    private void cargarDatosIniciales() {
        cmbEspecialidad.addItem("Seleccionar Especialidad");
        String queryEsp = "SELECT IdEspecialidad, NombreEspecialidad FROM Especialidades ORDER BY NombreEspecialidad";
        try (Connection conn = conexion.getConexion(); Statement stm = conn.createStatement(); ResultSet rs = stm.executeQuery(queryEsp)) {
            while (rs.next()) {
                Especialidad e = new Especialidad(rs.getInt("IdEspecialidad"), rs.getString("NombreEspecialidad"));
                especialidadesDisponibles.add(e);
                cmbEspecialidad.addItem(e.getNombre());
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al cargar especialidades: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error al cargar especialidades.", "Error DB", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarCampos() {
        txtIdMedico.setText("");
        txtNombre.setText("");
        txtApellido.setText("");
        txtNumeroMatricula.setText("");
        cmbEspecialidad.setSelectedIndex(0);
        txtTelefono.setText("");
        tblMedicos.clearSelection();
    }

    private void guardarMedico() {
        try {
            String nombre = txtNombre.getText();
            String apellido = txtApellido.getText();
            String numeroMatricula = txtNumeroMatricula.getText();

            int selectedEspecialidadIndex = cmbEspecialidad.getSelectedIndex();
            if (selectedEspecialidadIndex <= 0) {
                JOptionPane.showMessageDialog(this, "Seleccione una especialidad.", "Especialidad Vacia", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Especialidad especialidad = especialidadesDisponibles.get(selectedEspecialidadIndex - 1);

            String telefono = txtTelefono.getText();

            if (nombre.isEmpty() || apellido.isEmpty() || numeroMatricula.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nombre, apellido y numero de matricula son obligatorios.", "Datos Faltantes", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Medico medico = new Medico(nombre, apellido, numeroMatricula, especialidad, telefono);

            Connection conn = null;
            try {
                conn = conexion.getConexion();
                if (conn == null) {
                    throw new SQLException("No hay conexion a la base de datos.");
                }
                conn.setAutoCommit(false);

                if (txtIdMedico.getText().isEmpty()) {
                    if (medicoExisteEnBD(numeroMatricula)) {
                        JOptionPane.showMessageDialog(this, "Ya existe un medico con la matricula " + numeroMatricula + ".", "Matricula Duplicada", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    String insertSQL = "INSERT INTO Medicos (Nombre, Apellido, NumeroMatricula, IdEspecialidad, Telefono) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
                        pstmt.setString(1, medico.getNombre());
                        pstmt.setString(2, medico.getApellido());
                        pstmt.setString(3, medico.getNumeroMatricula());
                        pstmt.setInt(4, medico.getEspecialidad().getId());
                        pstmt.setString(5, medico.getTelefono());
                        conexion.ejecutarUpdate(pstmt);
                        try (ResultSet rs = pstmt.getGeneratedKeys()) {
                            if (rs.next()) {
                                medico.setIdMedico(rs.getInt(1));
                            }
                        }
                    }
                    JOptionPane.showMessageDialog(this, "Medico registrado con exito.", "Exito", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    medico.setIdMedico(Integer.parseInt(txtIdMedico.getText()));
                    String updateSQL = "UPDATE Medicos SET Nombre = ?, Apellido = ?, NumeroMatricula = ?, IdEspecialidad = ?, Telefono = ? WHERE IdMedico = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
                        pstmt.setString(1, medico.getNombre());
                        pstmt.setString(2, medico.getApellido());
                        pstmt.setString(3, medico.getNumeroMatricula());
                        pstmt.setInt(4, medico.getEspecialidad().getId());
                        pstmt.setString(5, medico.getTelefono());
                        pstmt.setInt(6, medico.getIdMedico());
                        conexion.ejecutarUpdate(pstmt);
                    }
                    JOptionPane.showMessageDialog(this, "Medico actualizado con exito.", "Exito", JOptionPane.INFORMATION_MESSAGE);
                }
                conn.commit();
                cargarMedicosEnTabla();
                limpiarCampos();
            } catch (SQLException e) {
                try {
                    System.err.println("ERROR: Fallo al guardar medico en BD: " + e.getMessage());
                    if (conn != null && !conn.isClosed()) {
                        try {
                            conn.rollback();
                        } catch (SQLException ex) {
                            System.err.println("ERROR: Fallo en rollback: " + ex.getMessage());
                        }
                    }
                    JOptionPane.showMessageDialog(this, "Error al guardar medico: " + e.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
                } catch (SQLException ex) {
                    Logger.getLogger(ABMMedicosForm.class.getName()).log(Level.SEVERE, null, ex);
                }
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
                    Logger.getLogger(ABMMedicosForm.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "ID de medico invalido.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarMedico() {
        int selectedRow = tblMedicos.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un medico de la tabla para eliminar.", "Sin Seleccion", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int idMedico = (Integer) tablaMedicosModel.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this, "¿Esta seguro de eliminar este medico? Esto tambien eliminara sus citas y horarios.", "Confirmar Eliminacion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            Connection conn = null;
            try {
                conn = conexion.getConexion();
                if (conn == null) {
                    throw new SQLException("No hay conexion a la base de datos.");
                }
                conn.setAutoCommit(false);

                String deleteCitasSQL = "DELETE FROM Citas WHERE IdMedico = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteCitasSQL)) {
                    pstmt.setInt(1, idMedico);
                    pstmt.executeUpdate();
                }
                String deleteHorariosSQL = "DELETE FROM HorariosMedicos WHERE IdMedico = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteHorariosSQL)) {
                    pstmt.setInt(1, idMedico);
                    pstmt.executeUpdate();
                }
                String updateUserSQL = "UPDATE Usuarios SET IdMedicoAsociado = NULL WHERE IdMedicoAsociado = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateUserSQL)) {
                    pstmt.setInt(1, idMedico);
                    pstmt.executeUpdate();
                }

                String deleteMedicoSQL = "DELETE FROM Medicos WHERE IdMedico = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteMedicoSQL)) {
                    pstmt.setInt(1, idMedico);
                    pstmt.executeUpdate();
                }
                conn.commit();
                JOptionPane.showMessageDialog(this, "Medico eliminado con exito.", "Exito", JOptionPane.INFORMATION_MESSAGE);
                cargarMedicosEnTabla();
                limpiarCampos();
            } catch (SQLException e) {
                try {
                    System.err.println("ERROR: Fallo al eliminar medico en BD: " + e.getMessage());
                    if (conn != null && !conn.isClosed()) {
                        try {
                            conn.rollback();
                        } catch (SQLException ex) {
                            System.err.println("ERROR: Fallo en rollback: " + ex.getMessage());
                        }
                    }
                    JOptionPane.showMessageDialog(this, "Error al eliminar medico: " + e.getMessage() + "\nAsegurese que no haya otras restricciones (ej. en usuarios).", "Error BD", JOptionPane.ERROR_MESSAGE);
                } catch (SQLException ex) {
                    Logger.getLogger(ABMMedicosForm.class.getName()).log(Level.SEVERE, null, ex);
                }
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
                    Logger.getLogger(ABMMedicosForm.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void cargarMedicosEnTabla() {
        tablaMedicosModel.setRowCount(0);
        String query = "SELECT m.IdMedico, m.Nombre, m.Apellido, m.NumeroMatricula, e.NombreEspecialidad, m.Telefono FROM Medicos m JOIN Especialidades e ON m.IdEspecialidad = e.IdEspecialidad ORDER BY m.Apellido, m.Nombre";
        try (Connection conn = conexion.getConexion(); Statement stm = conn.createStatement(); ResultSet rs = stm.executeQuery(query)) {
            while (rs.next()) {
                tablaMedicosModel.addRow(new Object[]{
                    rs.getInt("IdMedico"),
                    rs.getString("Nombre"),
                    rs.getString("Apellido"),
                    rs.getString("NumeroMatricula"),
                    rs.getString("NombreEspecialidad"),
                    rs.getString("Telefono")
                });
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al cargar medicos en tabla: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error al cargar medicos: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarMedicoSeleccionado() {
        int selectedRow = tblMedicos.getSelectedRow();
        if (selectedRow != -1) {
            int idMedico = (Integer) tablaMedicosModel.getValueAt(selectedRow, 0);
            String query = "SELECT m.IdMedico, m.Nombre, m.Apellido, m.NumeroMatricula, m.Telefono, e.IdEspecialidad, e.NombreEspecialidad FROM Medicos m JOIN Especialidades e ON m.IdEspecialidad = e.IdEspecialidad WHERE m.IdMedico = ?";
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                conn = conexion.getConexion();
                if (conn == null) {
                    throw new SQLException("No hay conexion a la base de datos.");
                }
                pstmt = conn.prepareStatement(query);

                pstmt.setInt(1, idMedico); // <<-- AQUI LA CORRECCION: Setea el parametro 1
                rs = conexion.ejecutarQuery(pstmt);

                if (rs.next()) {
                    txtIdMedico.setText(String.valueOf(rs.getInt("IdMedico")));
                    txtNombre.setText(rs.getString("Nombre"));
                    txtApellido.setText(rs.getString("Apellido"));
                    txtNumeroMatricula.setText(rs.getString("NumeroMatricula"));
                    txtTelefono.setText(rs.getString("Telefono"));

                    int idEspecialidad = rs.getInt("IdEspecialidad");
                    for (int i = 0; i < especialidadesDisponibles.size(); i++) {
                        if (especialidadesDisponibles.get(i).getId() == idEspecialidad) {
                            cmbEspecialidad.setSelectedIndex(i + 1);
                            break;
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("ERROR: Fallo al mostrar medico seleccionado: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error al cargar detalles del medico: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
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

    private boolean medicoExisteEnBD(String numeroMatricula) {
        String query = "SELECT COUNT(*) FROM Medicos WHERE NumeroMatricula = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = conexion.getConexion();
            if (conn == null) {
                throw new SQLException("No hay conexion a la base de datos.");
            }
            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, numeroMatricula); // <<-- AQUI LA CORRECCION: Setea el parametro 1
            rs = conexion.ejecutarQuery(pstmt);
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar matricula duplicada: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error de BD al verificar matricula.", "Error DB", JOptionPane.ERROR_MESSAGE);
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
        txtIdMedico = new javax.swing.JTextField();
        txtNombre = new javax.swing.JTextField();
        txtApellido = new javax.swing.JTextField();
        txtNumeroMatricula = new javax.swing.JTextField();
        txtTelefono = new javax.swing.JTextField();
        cmbEspecialidad = new javax.swing.JComboBox<>();
        btnNuevo = new javax.swing.JButton();
        btnGuardar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblMedicos = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        jLabel1.setText("ID Medico:");

        jLabel2.setText("Nombre:");

        jLabel3.setText("Apellido:");

        jLabel4.setText("Matricula:");

        jLabel5.setText("Especialidad:");

        jLabel6.setText("Telefono:");

        txtIdMedico.setEditable(false);
        txtIdMedico.setEnabled(false);

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
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtTelefono, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtIdMedico, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(txtNombre, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtApellido, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(6, 6, 6)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtNumeroMatricula, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cmbEspecialidad, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnNuevo, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnGuardar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEliminar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(22, 22, 22))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtIdMedico, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(txtApellido, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(txtNumeroMatricula, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(cmbEspecialidad, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(txtTelefono, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btnNuevo, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(33, 33, 33)
                        .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(33, 33, 33)
                        .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(0, 12, Short.MAX_VALUE))
        );

        tblMedicos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Nombre", "Apellido", "Matricula", "Especialidad", "Telefono"
            }
        ));
        jScrollPane1.setViewportView(tblMedicos);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 800, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 21, Short.MAX_VALUE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(19, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        eliminarMedico();
    }//GEN-LAST:event_btnEliminarActionPerformed

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        guardarMedico();
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevoActionPerformed
        limpiarCampos();
    }//GEN-LAST:event_btnNuevoActionPerformed

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
            java.util.logging.Logger.getLogger(ABMMedicosForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ABMMedicosForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ABMMedicosForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ABMMedicosForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ABMMedicosForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JComboBox<String> cmbEspecialidad;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tblMedicos;
    private javax.swing.JTextField txtApellido;
    private javax.swing.JTextField txtIdMedico;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtNumeroMatricula;
    private javax.swing.JTextField txtTelefono;
    // End of variables declaration//GEN-END:variables
}
