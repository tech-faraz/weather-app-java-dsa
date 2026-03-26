import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Stack;

public class WeatherAppGui extends JFrame {
    private JSONObject weatherData;
    private JLabel weatherConditionImage;
    private Stack<String> searchHistory = new Stack<>();
    private static final int MAX_HISTORY_SIZE = 15;
    private static final String HISTORY_FILE = "history.json";

    public WeatherAppGui() {
        super("Weather App");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(450, 700);
        setLocationRelativeTo(null);
        setResizable(false);

        setContentPane(new BackgroundPanel("src/assets/after.png"));

        setLayout(null);


        searchHistory = loadSearchHistory();

        addGuiComponents();
    }

    private void addGuiComponents() {
        JTextField searchTextField = new JTextField();
        searchTextField.setBounds(15, 15, 351, 45);
        searchTextField.setFont(new Font("Dialog", Font.PLAIN, 24));
        add(searchTextField);

        weatherConditionImage = new JLabel(loadImage("src/assets/cloudy.png"));
        weatherConditionImage.setBounds(0, 125, 450, 217);
        add(weatherConditionImage);

        JLabel temperatureText = new JLabel("10°C");
        temperatureText.setBounds(0, 350, 450, 54);
        temperatureText.setFont(new Font("Dialog", Font.BOLD, 48));
        temperatureText.setHorizontalAlignment(SwingConstants.CENTER);
        add(temperatureText);

        JLabel weatherConditionDesc = new JLabel("Cloudy");
        weatherConditionDesc.setBounds(0, 405, 450, 36);
        weatherConditionDesc.setFont(new Font("Dialog", Font.PLAIN, 32));
        weatherConditionDesc.setHorizontalAlignment(SwingConstants.CENTER);
        add(weatherConditionDesc);

        JLabel humidityImage = new JLabel(loadImage("src/assets/humidity.png"));
        humidityImage.setBounds(15, 500, 74, 66);
        add(humidityImage);

        JLabel humidityText = new JLabel("<html><b>Humidity</b> 100%</html>");
        humidityText.setBounds(90, 500, 85, 55);
        humidityText.setFont(new Font("Dialog", Font.PLAIN, 16));
        add(humidityText);

        JLabel windspeedImage = new JLabel(loadImage("src/assets/windspeed.png"));
        windspeedImage.setBounds(220, 500, 74, 66);
        add(windspeedImage);

        JLabel windspeedText = new JLabel("<html><b>Windspeed</b> 15km/h</html>");
        windspeedText.setBounds(310, 500, 85, 55);
        windspeedText.setFont(new Font("Dialog", Font.PLAIN, 16));
        add(windspeedText);

        JButton searchButton = new JButton(loadImage("src/assets/search.png"));
        searchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchButton.setBounds(375, 13, 47, 45);
        searchButton.addActionListener(e -> {
            String location = searchTextField.getText();
            JSONObject weatherData = WeatherApp.getWeatherData(location);

            if (weatherData != null) {
                temperatureText.setText(weatherData.get("temperature") + "°C");
                weatherConditionDesc.setText((String) weatherData.get("weather_condition"));
                humidityText.setText("<html><b>Humidity</b> " + weatherData.get("humidity") + "%</html>");
                windspeedText.setText("<html><b>Windspeed</b> " + weatherData.get("windspeed") + " m/s</html>");

                updateWeatherImage((String) weatherData.get("weather_condition"));

                updateSearchHistory(location);
            } else {
                JOptionPane.showMessageDialog(this, "Weather data could not be fetched.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        add(searchButton);


        JButton historyButton = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                ImageIcon historyIcon = new ImageIcon("src/assets/History.png");
                g.drawImage(historyIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
            }
        };
        historyButton.setBounds(300, 70, 120, 45);
        historyButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        historyButton.setContentAreaFilled(false);
        historyButton.setBorderPainted(false);

        historyButton.addActionListener(e -> showSearchHistory());
        add(historyButton);

    }

    private void updateSearchHistory(String location) {
        if (searchHistory.size() >= MAX_HISTORY_SIZE) {
            searchHistory.remove(0);
        }
        searchHistory.push(location);
        saveSearchHistory();
    }

    private void showSearchHistory() {

        this.setVisible(false);

        JFrame historyFrame = new JFrame("Search History");
        historyFrame.setSize(400, 600);
        historyFrame.setLocationRelativeTo(this);
        historyFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        historyFrame.setLayout(new BorderLayout());

        if (searchHistory.isEmpty()) {

            JLabel noHistoryLabel = new JLabel("History not available", SwingConstants.CENTER);
            noHistoryLabel.setFont(new Font("Dialog", Font.PLAIN, 20));
            historyFrame.add(noHistoryLabel, BorderLayout.CENTER);
        } else {

            DefaultListModel<String> listModel = new DefaultListModel<>();
            for (String city : searchHistory) {
                listModel.addElement(city);
            }

            JList<String> historyList = new JList<>(listModel);
            historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            historyList.setFont(new Font("Dialog", Font.PLAIN, 16));
            JScrollPane scrollPane = new JScrollPane(historyList);
            historyFrame.add(scrollPane, BorderLayout.CENTER);
        }


        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        JButton clearHistoryButton = new JButton("Clear History");
        clearHistoryButton.addActionListener(e -> {
            if (searchHistory.isEmpty()) {
                JOptionPane.showMessageDialog(historyFrame, "History not available.", "Warning", JOptionPane.WARNING_MESSAGE);
            } else {
                searchHistory.clear();
                saveSearchHistory();


                JOptionPane.showMessageDialog(historyFrame, "History cleared successfully.", "Info", JOptionPane.INFORMATION_MESSAGE);


                historyFrame.getContentPane().removeAll();
                JLabel noHistoryLabel = new JLabel("History not available", SwingConstants.CENTER);
                noHistoryLabel.setFont(new Font("Dialog", Font.PLAIN, 20));
                historyFrame.add(noHistoryLabel, BorderLayout.CENTER);
                historyFrame.revalidate();
                historyFrame.repaint();
            }
        });


        JButton deleteSelectedButton = new JButton("Delete");
        deleteSelectedButton.addActionListener(e -> {
            if (searchHistory.isEmpty()) {
                JOptionPane.showMessageDialog(historyFrame, "History not available.", "Warning", JOptionPane.WARNING_MESSAGE);
            } else {
                // Get the selected index from the history list
                JScrollPane scrollPane = (JScrollPane) historyFrame.getContentPane().getComponent(0);
                JList<String> historyList = (JList<String>) scrollPane.getViewport().getView();
                int selectedIndex = historyList.getSelectedIndex();

                if (selectedIndex != -1) {
                    // Remove the selected entry from the history stack
                    searchHistory.remove(selectedIndex);
                    saveSearchHistory(); // Save the updated history
                    JOptionPane.showMessageDialog(historyFrame, "Entry deleted successfully.", "Info", JOptionPane.INFORMATION_MESSAGE);

                    // Refresh the JList to reflect the changes
                    DefaultListModel<String> listModel = (DefaultListModel<String>) historyList.getModel();
                    listModel.remove(selectedIndex);

                    // Check if history is empty after deletion
                    if (searchHistory.isEmpty()) {
                        // Update UI to show "History not available"
                        historyFrame.getContentPane().removeAll();
                        JLabel noHistoryLabel = new JLabel("History not available", SwingConstants.CENTER);
                        noHistoryLabel.setFont(new Font("Dialog", Font.PLAIN, 20));
                        historyFrame.add(noHistoryLabel, BorderLayout.CENTER);
                        historyFrame.revalidate();
                        historyFrame.repaint();
                    }
                } else {
                    JOptionPane.showMessageDialog(historyFrame, "No entry selected.", "Warning", JOptionPane.WARNING_MESSAGE);
                }
            }
        });


        buttonPanel.add(clearHistoryButton);
        buttonPanel.add(deleteSelectedButton);
        historyFrame.add(buttonPanel, BorderLayout.SOUTH);

        // Add a listener to restore the main WeatherAppGui frame when the history frame is closed
        historyFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                WeatherAppGui.this.setVisible(true); // Show the main frame
            }
        });

        // Show the history frame
        historyFrame.setVisible(true);
    }



    private void saveSearchHistory() {
        JSONArray historyArray = new JSONArray();
        historyArray.addAll(searchHistory);

        try (FileWriter writer = new FileWriter(HISTORY_FILE)) {
            writer.write(historyArray.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Stack<String> loadSearchHistory() {
        Stack<String> stack = new Stack<>();
        File file = new File(HISTORY_FILE);
        if (!file.exists()) {
            return stack; // Return empty stack if file doesn't exist
        }

        try (FileReader reader = new FileReader(file)) {
            JSONParser parser = new JSONParser();
            JSONArray historyArray = (JSONArray) parser.parse(reader);

            for (Object location : historyArray) {
                stack.push(location.toString());
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return stack;
    }

    private void updateWeatherImage(String weatherCondition) {
        String imagePath = "src/assets/cloudy.png";

        switch (weatherCondition.toLowerCase()) {
            case "clear":
                imagePath = "src/assets/clear.png";
                break;
            case "cloudy":
                imagePath = "src/assets/cloudy.png";
                break;
            case "rain":
                imagePath = "src/assets/rain.png";
                break;
            case "snow":
                imagePath = "src/assets/snow.png";
                break;
            default:
                imagePath = "src/assets/unknown.png";
                break;
        }

        weatherConditionImage.setIcon(loadImage(imagePath));
    }

    private ImageIcon loadImage(String imagePath) {
        try {
            BufferedImage img = ImageIO.read(new File(imagePath));
            return new ImageIcon(img);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static class BackgroundPanel extends JPanel {
        private final Image backgroundImage;

        public BackgroundPanel(String imagePath) {
            this.backgroundImage = new ImageIcon(imagePath).getImage();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}




