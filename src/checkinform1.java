import java.awt.*;
import java.awt.event.*;
import java.rmi.registry.LocateRegistry;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

import javax.swing.*;

public class checkinform1 extends JFrame {

    private Connection connection;
    private JTextField cnpTextField;
    private String numarOrdine;
    private String oraCheckIn;


    public checkinform1() {
        connection = null;
        
            
        

        // Setări fereastră
        setTitle("Verificare Check-in");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(400, 150));

        // Creare componente
        JLabel cnpLabel = new JLabel("CNP:");
        cnpTextField = new JTextField(10);
        JButton checkinButton = new JButton("Verifică");

        // Adăugare ascultător pentru butonul de verificare
        checkinButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String cnp = cnpTextField.getText();
                try {
                    if (checkCnpValidity(cnp)) {
                        openSelectFlightForm(cnp);
                    } else {
                        JOptionPane.showMessageDialog(null, "CNP-ul introdus nu există în baza de date.");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "A apărut o eroare la verificarea CNP-ului.");
                }
            }
        });

        // Adăugare componente la container
        Container container = getContentPane();
        container.setLayout(new FlowLayout());
        container.add(cnpLabel);
        container.add(cnpTextField);
        container.add(checkinButton);

        // Conectare la baza de date
        String url = "jdbc:mysql://localhost:3306/aeroportu";
        String user = "root";
        String password = "";

        try {
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "A apărut o eroare la conectarea la baza de date.");
        }

        // Afișare fereastră
        pack();
        setVisible(true);
    }

    private boolean checkCnpValidity(String cnp) throws SQLException {
        String query = "SELECT nume, prenume FROM clienti WHERE cnp = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, cnp);
        ResultSet resultSet = statement.executeQuery();

        boolean isValid = resultSet.next();

        statement.close();
        resultSet.close();

        return isValid;
    }

    private void openSelectFlightForm(String cnp) {
        JFrame selectFlightForm = new JFrame("Selectare Zbor");
        selectFlightForm.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        selectFlightForm.setPreferredSize(new Dimension(400, 200));

        // Creare componente pentru selecția zborului și companiei de zbor
        JComboBox<String> zborComboBox = new JComboBox<>();
        JComboBox<String> companieComboBox = new JComboBox<>();
        JButton selectButton = new JButton("Selectează");

        // Ascultător pentru butonul de selectare
        selectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedZbor = (String) zborComboBox.getSelectedItem();
                String selectedCompanie = (String) companieComboBox.getSelectedItem();
                numarOrdine = generateRandomNumber();

                
                


                try {
                    String[] flightDetails = getFlightDetails(selectedZbor, selectedCompanie);
                    if (flightDetails != null) {
                        LocalDate dataZbor = LocalDate.parse(flightDetails[1]);
                        LocalTime oraPlecare = LocalTime.parse(flightDetails[2], DateTimeFormatter.ofPattern("HH:mm:ss"));
                        LocalDateTime oraPlecareDateTime = LocalDateTime.of(dataZbor, oraPlecare);
                        LocalDateTime oraCheckInDateTime = oraPlecareDateTime.minusHours(2);
                        String oraCheckIn = oraCheckInDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                        showConfirmationDialog(cnp, selectedZbor, selectedCompanie, flightDetails, oraCheckIn);
                        
                    } else {
                        JOptionPane.showMessageDialog(null, "Nu s-au găsit informații pentru zborul selectat.");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "A apărut o eroare la obținerea informațiilor despre zbor.");
                }
            }
        });

        // Adăugare componente la fereastra de selecție
        Container container = selectFlightForm.getContentPane();
        container.setLayout(new FlowLayout());
        container.add(new JLabel("Zbor:"));
        container.add(zborComboBox);
        container.add(new JLabel("Companie de zbor:"));
        container.add(companieComboBox);
        container.add(selectButton);

        // Inițializare combobox-uri cu datele corespunzătoare din baza de date
        try {
            populateZborComboBox(zborComboBox);
            populateCompanieComboBox(companieComboBox);
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "A apărut o eroare la inițializarea combobox-urilor.");
            selectFlightForm.dispose(); // Închide fereastra de selecție a zborului și companiei de zbor în caz de eroare
        }

        // Afișare fereastră de selecție a zborului și companiei de zbor
        selectFlightForm.pack();
        selectFlightForm.setVisible(true);
    }

    private void populateZborComboBox(JComboBox<String> zborComboBox) throws SQLException {
        String query = "SELECT Nume_zbor FROM zboruri";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);

        while (resultSet.next()) {
            String idZbor = resultSet.getString("Nume_zbor");
            zborComboBox.addItem(idZbor);
        }

        statement.close();
        resultSet.close();
    }

    private void populateCompanieComboBox(JComboBox<String> companieComboBox) throws SQLException {
        String query = "SELECT nume FROM companii";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);

        while (resultSet.next()) {
            String denumire = resultSet.getString("nume");
            companieComboBox.addItem(denumire);
        }

        statement.close();
        resultSet.close();
    }
     

    private String[] getFlightDetails(String selectedZbor, String selectedCompanie) throws SQLException {
        String query = "SELECT aeroport_plecare, aeroport_sosire, data_zbor, ora_plecare, ora_sosire FROM zboruri WHERE Nume_zbor = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, selectedZbor);
        ResultSet resultSet = statement.executeQuery();
    
        String[] flightDetails = null;
    
        if (resultSet.next()) {
            String aeroportPlecare = resultSet.getString("aeroport_plecare");
            String aeroportSosire = resultSet.getString("aeroport_sosire");
            String dataZbor = resultSet.getString("data_zbor");
            String oraPlecare = resultSet.getString("ora_plecare");
            String oraSosire = resultSet.getString("ora_sosire");
    
            String locatiePlecare = getAirportLocation(aeroportPlecare, "locatie");
            String locatieSosire = getAirportLocation(aeroportSosire, "locatie");
    
            flightDetails = new String[]{aeroportSosire, dataZbor, oraPlecare, oraSosire, locatiePlecare, locatieSosire};
        }
    
        statement.close();
        resultSet.close();
    
        return flightDetails;
    }

    
    
    
    private int generateOrderNumber() {
        return 0;
    }

    private String getAirportLocation(String airportValue, String column) throws SQLException {
        String query = "SELECT locatie FROM aeroporturi WHERE nume = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, airportValue);
        ResultSet resultSet = statement.executeQuery();
    
        String location = null;
        if (resultSet.next()) {
            location = resultSet.getString("locatie");
        }
    
        statement.close();
        resultSet.close();
    
        return location;
    }
    private String generateRandomNumber() {
    int min = 1000; // Valoarea minimă a numărului de ordine
    int max = 9999; // Valoarea maximă a numărului de ordine
    int randomNumber = (int) (Math.random() * (max - min + 1) + min);
    return String.valueOf(randomNumber);
}


    
    
    

private void showConfirmationDialog(String cnp, String selectedZbor, String selectedCompanie, String[] flightDetails, String oraCheckIn) {
        String aeroportSosire = flightDetails[0];
        String dataZbor = flightDetails[1];
        String oraPlecare = flightDetails[2];
        String oraSosire = flightDetails[3];
        String locatiePlecare = flightDetails[4];
        String locatieSosire = flightDetails[5];
        

         // Obțineți numele și prenumele clientului
    String nume = "";
    String prenume = "";
    try {
        String query = "SELECT nume, prenume FROM clienti WHERE cnp = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, cnp);
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            nume = resultSet.getString("nume");
            prenume = resultSet.getString("prenume");
        }

        statement.close();
        resultSet.close();
    } catch (SQLException ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, "A apărut o eroare la obținerea informațiilor despre client.");
    }

        String message = "CNP-ul a fost verificat cu succes.\n" +
                "Numar Ordine: " + numarOrdine +  "\n" +
                "Nume: " + nume + "\n" +
                "Prenume: " + prenume + "\n" +
                "Nume zbor selectat: " + selectedZbor + "\n" +
                "Locație plecare: " + locatiePlecare + "\n" +
                "Companie de zbor: " + selectedCompanie + "\n" +
                "Aeroport sosire: " + aeroportSosire + "\n" +
                "Locație sosire: " + locatieSosire + "\n" +
                "Data zborului: " + dataZbor + "\n" +
                "Ora plecare: " + oraPlecare + "\n" +
                "Ora sosire: " + oraSosire + "\n" +
                "Ora Check-In: " + oraCheckIn;

        JOptionPane.showMessageDialog(null, message);

        try {
            String numarOrdine = generateRandomNumber(); // Generați numărul de ordine aleatoriu
    
            String query = "INSERT INTO checkin (numar_ordine, client_id, zbor_id, data_checkin, ora_checkin, Locatie, Destinatie) VALUES (?, (SELECT id FROM clienti WHERE cnp = ?), (SELECT id FROM zboruri WHERE Nume_zbor = ?), CURRENT_DATE(), ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, numarOrdine);
            statement.setString(2, cnp);
            statement.setString(3, selectedZbor);
            statement.setString(4, oraCheckIn);
            statement.setString(5, locatiePlecare);
            statement.setString(6, aeroportSosire);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "A apărut o eroare la inserarea înregistrării de check-in.");
        }
    }
    

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new checkinform1();
            }
        });
    }
}
