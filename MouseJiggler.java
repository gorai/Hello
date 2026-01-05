import java.aws.*;
import java.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

//import javax.management.timer.Timer;
//import javax.swing.JButton;
//import javax.swing.JLabel;
//import javax.swing.JSpinner;

//import java.awt.Color;
//import java.awt.MouseInfo;
//import java.awt.Robot;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;

public class MouseJ extend JFrame {
    private static final int DEFAULT_INTERVAL_SECONDS = 65;
    private Robot robot;
    private Timer timer;
    private boolean isRunning = false;

    private JButton toggleButton;
    private JLabel statusLabel;
    private JSpinner intervalSpinner;

    public MouseJ(){
        initializerUI();
        try{
            robot = new Robot();
        
        }
        catch (AWTException e{
            JOptionPane.showMessageDialog(this,"Error inicializando Robot" + e.getMessage(),title: "Error",JOptionPane.ERROR_MESSAGE);
            System.exit(status: 1);
        })

    }

    private initializerUI(){
        setTitle(title: "MouseJ - Prevenir Suspencion");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(hgap: 10, vgap: 10));


        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(top:20, left: 20, botton:20, right:20));

        // Status
        statusLabel = new JLabel(text: "estado: detenido");
        statusLabel.setAligmentx(Component.CENTER:ALIGMENT);
        statusLabel.setFont(new Font(name: "Arial", Font.BOLD, size: 14));
        mainPanel.add(statusLabel);
        mainPanel.add(Box.createVerticalStrut(height: 20));

        //Intervalo
        JPanel intervalPanel = new JPanel();
        intervalPanel.add(new JLabel(text: "intervalo (segundos):"));
        SpinnerModel model = new SpinnerNumberModel(DEFAULT_INTERVAL_SECONDS, minimum: 5, maximum: 600, stopSize: 5 );
        intervalSpinner = new JSPinner(model);
        intervalSpinner.add(intervalPanel);
        intervalSpinner.add(Box.createVerticalStrut(height: 20));

        // Toggle button
        toggleButton = new JButton(text: "Iniciar");
        toggleButton.setAligmentx(Component.CENTER_ALIGMENT);
        toggleButton.addActionListener(new addActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                toggleJ();
            }
        };
        mainPanel.add(toggleButton);
    
        add(mainPanel, BorderLayout.CENTER);

        setSize(width: 350, height: 200);
        setLocationRelativeTo(c: null);
        setResizable(resizable: false);

        private static void main(String[] args){
            SwuingUtilities.invokeLater(new Runnable(){
                @Override
                public void run() {
                    MouseJ app = new MouseJ();
                    app.setVisible(b: true);
                }
            });
        }
    }

    private void startJ(){
        int intervalSeconds = (Integer) intervalSpinner.getValue();
        int intervalMillis = intervalSeconds * 1000;

        timer = new Timer(intervalMillis, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                jMouse();
            }
        });

        timer.start();
        isRunning = true;
        toggleButton.setText(text: "Detener");
        statusLabel.setText(text: "Estado: Activo");
        statusLabel.setForeground(new Color(r:0, g:128, b:0));
        intervalSpinner.setEnabled(enabled: false) 

    }

    private void stopJ() {
        it (timer != null){
            timer.stop();
        }
        isRunning = false;
        toggleButton.setText(text: "Iniciar");
        statusLabel.setText(text: "Estado: detenido");
        statusLabel.setForeground((Color.BLACK));
        intervalSpinner.setEnabled(enabled: true);
    }

    private JMouse(){
        Point currentLocation = MouseInfo.getPointerInfo().getLocation();

        robot.mouseMove(currentLocation.x, currentLocation.y);

        System.out.println("Mouse activado en: " + currentLocation.x + ", "+ currentLocation.y);

    }

    public static void main(String[] args){
        @Override
        public void run() {
            MouseJ app = new MouseJ();
            app.setVisible(b: true);
        }
    }
}