import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.swing.*;

class KeyboardZoomAction extends AbstractAction {

    boolean isZoomOut;

    public KeyboardZoomAction(Boolean b) {
        isZoomOut = b;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(isZoomOut)
            OPC.scale *= 1.1;
        else
            OPC.scale /= 1.1;


    }

}

public class OPC extends JComponent {

    public static JFrame f;
    public static JLabel l;
    public static OPC opc;

    public static double defaultS = 200;
    public static double k = 200;
    public static double iv = .6;
    public static double t = .05;
    public static double r = .03;
    public static double q = .01;

    public static double[] optionVars = {defaultS, k, iv, t, r, q};


    public static double scale = 1;

    public static int mouseX;
    public static int mouseY;
    public static double[] ys;

    public static JPanel greeksPanel;
    public static JPanel slidersPanel;

    public static JButton restoreDefaults;
    public static JButton restoreZoom;

    public static JRadioButton[] greeksButtons = {new JRadioButton("(S, ∂V/∂S)"), new JRadioButton("(S, ∂²V/∂S²)"),
            new JRadioButton("(S, ∂V/∂t)"), new JRadioButton("(σ, ∂V/∂σ)"),
            new JRadioButton("(t, V)"), new JRadioButton("(σ, V)"),
            new JRadioButton("(S, ∂²v/∂σ²)"), new JRadioButton("(S, V)"),
            new JRadioButton("(S, ∂V/∂σ)"), new JRadioButton("(S, ∂²V/∂t∂S)"),
            new JRadioButton("(S, ∂²V/∂S∂σ)"), new JRadioButton("(S, λ)")};

    public static JSlider[] sliders = {new JSlider(JSlider.HORIZONTAL, 0, 500, 200), new JSlider(JSlider.HORIZONTAL, 0, 500, 200),
            new JSlider(JSlider.HORIZONTAL, 0, 500, 60), new JSlider(JSlider.HORIZONTAL, 0, 500, 18),
            new JSlider(JSlider.HORIZONTAL, 0, 2000, 300), new JSlider(JSlider.HORIZONTAL, 0, 1500, 100)};


    public static JLabel[] sliderLabels = {new JLabel("Stock Price:"), new JLabel("Strike Price:"),
            new JLabel("Implied Volatility:"), new JLabel("Days till Exp.:"),
            new JLabel("Interest Rate:"), new JLabel("Dividend Yield")};
    public static JLabel[] sliderValues;
    public static Polygon p = new Polygon();
    public static boolean isNewGraph;
    private static final int IFW = JComponent.WHEN_IN_FOCUSED_WINDOW;

    public OPC(boolean b)
    {
        isNewGraph = b;
    }

    public void setFocusable(boolean b) {
        super.setFocusable(b);
    }

    public static double CNDF(double x)
    {
        int neg = (x < 0d) ? 1 : 0;
        if ( neg == 1)
            x *= -1d;

        double k = (1d / ( 1d + 0.2316419 * x));
        double y = (((( 1.330274429 * k - 1.821255978) * k + 1.781477937) *
                k - 0.356563782) * k + 0.319381530) * k;
        y = 1.0 - 0.398942280401 * Math.exp(-0.5 * x * x) * y;

        return (1d - neg) * y + neg * (1d - y);
    }

    public static double PDF(double x)
    {
        return (Math.exp(-.5 * x * x) / Math.sqrt(2 * Math.PI));
    }

    public static double d1StockPrice(double s)
    {
        double x = Math.log(s / k);
        double mu = (r - q + .5 * iv * iv) * t;
        double sigma = iv * Math.sqrt(t);
        return (x + mu) / sigma;
    }

    public static double d1IV(double IV)
    {
        double x = Math.log(defaultS / k);
        double mu = (r - q + .5 * IV * IV) * t;
        double sigma = IV * Math.sqrt(t);
        return (x + mu) / sigma;
    }

    public static double d1Time(double time)
    {
        double x = Math.log(defaultS / k);
        double mu = (r - q + .5 * iv * iv) * time;
        double sigma = iv * Math.sqrt(time);
        return (x + mu) / sigma;
    }

    public static double d2StockPrice(double s)
    {
        return d1StockPrice(s) - iv * Math.sqrt(t);
    }

    public static double d2IV(double IV)
    {
        return d1IV(IV) - IV * Math.sqrt(t);
    }

    public static double d2Time(double time)
    {
        return d1Time(time) - iv * Math.sqrt(time);
    }

    public static double round(double value, int places)
    {
        if(Double.isNaN(value))
            return 0;
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static void updateGraph(boolean b)
    {
        f.remove(opc);
        p.reset();
        OPC nopc = new OPC(b);
        f.add(nopc);
    }

    public static void updateOptionVars()
    {
        defaultS = optionVars[0];
        k = optionVars[1];
        iv = optionVars[2];
        t = optionVars[3];
        r = optionVars[4];
        q = optionVars[5];
    }

    public static void pcHelper(Graphics2D g1, Graphics2D g2, int w, int h, String xString, String yString, boolean posAndNeg)
    {
        g2.drawPolyline(p.xpoints, p.ypoints, p.npoints);
        g1.setStroke(new BasicStroke(2));
        g1.setColor(Color.black);
        g1.drawLine(0, h, 2 * w, h);
        g1.drawLine(0, 0, 0, 2 * h);
        g1.drawString("0", 0, h + 13);
        for(int i = 20; i < 500; i += 20)
            g1.drawString(Integer.toString(i), i - 7, h + 13);
        g1.drawString("^ " + xString, 500, h + 13);
        for(int i = 10; i < 210; i += 10)
            g1.drawString(Double.toString(round((double)i/(scale), 3)), 0, h - (int)(1.5 * i) + 2);
        if(posAndNeg)
            for(int i = -10; i > -210; i -= 10)
                g1.drawString(Double.toString(round((double)i/(scale), 3)), 0, h - (int)(1.5 * i) + 2);
        g1.drawString("< " + yString, 10, 10);
    }

    public static void setLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("CrossPlatform".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(OPC.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }

    public static void initFrame()
    {
        //frame setup
        f = new JFrame("WSB Tendie Analyzer");
        f.setSize(1100, 700);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setFocusable(true);
        f.setVisible(true);
        setLookAndFeel();

        f.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                frameMouseMoved(e);
            }
        });

        f.addMouseWheelListener(OPC::frameMouseWheelMoved);


        //graph renderer setup
        opc = new OPC(true);
        opc.setFocusable(true);

        //keyboard listener label setup
        l = new JLabel();
        l.setSize(100, 60);
        l.setBorder(BorderFactory.createLineBorder(Color.black));

        l.getInputMap(IFW).put(KeyStroke.getKeyStroke("W"), "zoom out");
        l.getInputMap(IFW).put(KeyStroke.getKeyStroke("S"), "zoom in");

        l.getActionMap().put("zoom out", new KeyboardZoomAction(true));
        l.getActionMap().put("zoom in", new KeyboardZoomAction(false));

        //greeks button container setup
        greeksPanel = new JPanel();
        greeksPanel.setBounds(830, 0, 250, 200);
        greeksPanel.setBorder(BorderFactory.createLineBorder(Color.black));

        //option variable sliders container setup
        slidersPanel = new JPanel();
        slidersPanel.setBounds(640, 350, 440, 300);
        slidersPanel.setBorder(BorderFactory.createLineBorder(Color.black));

        //option variable slider labels setup
        for(int i = 0; i < sliderLabels.length; i++)
        {
            sliderLabels[i].setBounds(500, 350 + (i * 50), 100, 30);
            f.add(sliderLabels[i]);
        }

        //option variable slider value display setup
        sliderValues = new JLabel[6];
        for(int i = 0; i < sliderValues.length; i++)
        {
            sliderValues[i] = new JLabel(Double.toString(sliders[i].getValue()));
            sliderValues[i].setBounds(600, 350 + (i * 50), 40, 30);
            f.add(sliderValues[i]);
        }

        StringBuilder sb = new StringBuilder(sliderValues[2].getText());
        sb.append("%");
        sliderValues[2].setText(sb.toString());
        sliderValues[3].setText(Double.toString(Double.parseDouble(sliderValues[3].getText())));
        for(int i = 4; i < sliderValues.length; i++)
        {
            sb = new StringBuilder(Double.toString(Double.parseDouble(sliderValues[i].getText()) / 100.0));
            sb.append("%");
            sliderValues[i].setText(sb.toString());
        }

        //restore default variable settings button setup
        restoreDefaults = new JButton("Restore Default Slider Settings");
        restoreDefaults.setBounds(830, 220, 250, 40);

        //restore default zoom button setup
        restoreZoom = new JButton("Restore Default Zoom Settings");
        restoreZoom.setBounds(830, 270, 250, 40);

        //some setup for greeks selector & button group
        greeksButtons[0].setSelected(true);
        ButtonGroup bg = new ButtonGroup();
        /*
         * adds each chart selector button to button group in order to
         * assure only one can be selected at a time & adds a simple
         * event listener to each one
         */
        for (JRadioButton greeksButton : greeksButtons) {
            bg.add(greeksButton);
            greeksPanel.add(greeksButton);
            greeksButton.addActionListener(e -> updateGraph(true));
        }

        //setup for option variable sliders
        for(int i = 0; i < sliders.length; i++){
            if(i < sliders.length - 2) {
                sliders[i].setMajorTickSpacing(50);
                sliders[i].setMinorTickSpacing(5);
            }
            else {
                sliders[i].setMajorTickSpacing(500);
                sliders[i].setMinorTickSpacing(50);
            }
            sliders[i].setPaintTicks(true);
            sliders[i].setPaintLabels(true);
            sliders[i].setFont(new Font(sliders[i].getFont().getName(), Font.PLAIN, 10));
            sliders[i].setPreferredSize(new Dimension(420, 45));
            slidersPanel.add(sliders[i]);

        }
        //adding simple change listeners for option variable sliders
        for(int i = 0; i < 2; i++)
        {
            final int i1 = i;
            sliders[i].addChangeListener(e -> {
                optionVars[i1] = sliders[i1].getValue();
                sliderValues[i1].setText(Integer.toString(sliders[i1].getValue()));
                updateOptionVars();
                updateGraph(true);
            });
        }
        sliders[2].addChangeListener(e -> {
            optionVars[2] = ((double)sliders[2].getValue() / 100.0);
            sliderValues[2].setText(sliders[2].getValue() + "%");
            updateOptionVars();
            updateGraph(true);
        });

        sliders[3].addChangeListener(e -> {
            optionVars[3] = sliders[3].getValue() / 365.0;
            sliderValues[3].setText(Integer.toString(sliders[3].getValue()));
            updateOptionVars();
            updateGraph(true);
        });

        for(int i = 4; i < sliders.length; i++)
        {
            //i1 constant declared to bypass scope issues - DO NOT TOUCH
            final int i1 = i;
            sliders[i].addChangeListener(e -> {
                optionVars[i1] = (sliders[i1].getValue() / 100.0) / 100.0;
                sliderValues[i1].setText(optionVars[i1] * 100.0 + "%");
                updateOptionVars();
                updateGraph(true);
            });
        }

        //adding components to frame
        f.add(l);
        f.add(greeksPanel);
        f.add(slidersPanel);
        f.add(restoreDefaults);
        f.add(restoreZoom);
        f.add(opc);

        restoreDefaults.addActionListener(OPC::restoreDefaultsActionPerformed);

        restoreZoom.addActionListener(e -> {
            scale = 1;
            updateGraph(false);
        });

        sliderValues[0].addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                sliderValues1ActionPerformed(e);
            }
            @Override
            public void mousePressed(MouseEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseEntered(MouseEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseExited(MouseEvent e) {
                // TODO Auto-generated method stub

            }
        });

        sliderValues[1].addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                sliderValues2ActionPerformed(e);
            }
            @Override
            public void mousePressed(MouseEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseEntered(MouseEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseExited(MouseEvent e) {
                // TODO Auto-generated method stub

            }

        });

    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        p.reset();
        int w = this.getWidth() / 2;
        int h = this.getHeight() / 2;
        Graphics2D g1 = (Graphics2D)g;
        g.setFont(new Font(g.getFont().getName(), Font.PLAIN, 10));
        Graphics2D g2 = (Graphics2D)g;

        g2.setStroke(new BasicStroke(2));
        g2.setColor(Color.red);
        ys = new double[501];
        if(isNewGraph || greeksButtons[0].isSelected())
            if(greeksButtons[0].isSelected())
            {
                for(int i = 0; i <= 500; i++)
                {
                    ys[i] = (100 * round(CNDF(d1StockPrice(i)), 4));
                    p.addPoint(i, h - (int)(3 * ys[i]));
                }
                g2.drawPolyline(p.xpoints, p.ypoints, p.npoints);
                g1.setStroke(new BasicStroke(2));
                g1.setColor(Color.black);
                g1.drawLine(0, h, 2 * w, h);
                g1.drawLine(0, 0, 0, 2 * h);
                g1.drawString("0", 0, h + 13);
                for(int i = 20; i < 500; i += 20)
                    g1.drawString(Integer.toString(i), i - 7, h + 13);
                g1.drawString("^ Stock Price", 500, h + 13);
                for(int i = 5; i < 105; i += 5)
                    g1.drawString(Integer.toString(i), 0, h - (3 * i) + 2);
                g1.drawString("< Delta", 10, 10);
                if(mouseX - 8 < 501 && mouseY - 30 < 701 && mouseX - 8 > 0)
                    g1.drawLine(mouseX-8, mouseY-30, mouseX-8, 330 - (int) (3 * ys[mouseX - 8]) );
            }
        if(greeksButtons[1].isSelected())
        {
            for(int i = 0; i <= 500; i++)
            {

                ys[i] = 100 * round(scale * Math.exp(-1 * q * t) * (PDF(d1StockPrice(i)) / (i * iv * Math.sqrt(t))), 4);
                p.addPoint(i, h - (int)(1.5 * ys[i]));
            }
            pcHelper(g1, g2, w, h, "Stock Price", "Gamma", false);
        }
        if(greeksButtons[2].isSelected())
        {
            for(int i = 0; i < 500; i++)
            {
                double t1 = -1 * Math.exp(-1 * q * t) * (i * PDF(d1StockPrice(i)) * iv) / (2 * Math.sqrt(t));
                double t2 = (r * k * Math.exp(-1 * r * t) * CNDF(d2StockPrice(i)));
                double t3 = (q * i * Math.exp(-1 * q * t) * CNDF(d1StockPrice(i)));
                ys[i] = round(scale * (t1 - t2 + t3) / 365.0, 4);
                p.addPoint(i, h - (int)(1.5 * ys[i]));
            }
            pcHelper(g1, g2, w, h, "Stock Price", "Theta", true);
        }
        if(greeksButtons[3].isSelected())
        {
            for(int i = 0; i < 500; i++)
            {
                ys[i] = round(scale * defaultS * Math.exp(-1 * q * t) * PDF(d1IV(((double)i / 100))) * Math.sqrt(t), 4);
                p.addPoint(i, h - (int)(1.5 * ys[i]));
            }
            pcHelper(g1, g2, w, h, "Implied Volatility (%)", "Vega", false);
        }
        if(greeksButtons[4].isSelected())
        {
            for(int i = 0; i < 500; i++)
            {
                ys[i] = round(((defaultS * Math.exp(-1 * q * ((double)i/365)) * CNDF(d1Time((double)i/365)) - k * Math.exp(-1 * r * ((double)i/365)) * CNDF(d2Time((double)i/365))) * scale), 4);
                p.addPoint(i, h - (int)(1.5 * ys[i]));
            }
            pcHelper(g1, g2, w, h, "Days till Expiration", "Option Value", false);
        }
        if(greeksButtons[5].isSelected())
        {
            for(int i = 0; i < 500; i++)
            {
                ys[i] = round(((defaultS * Math.exp(-1 * q * t) * CNDF(d1IV((double)i/100)) - k * Math.exp(-1 * r * t) * CNDF(d2IV((double)i/100))) * scale), 4);
                p.addPoint(i, h - (int)(1.5 * ys[i]));
            }
            pcHelper(g1, g2, w, h, "Implied Volatility (%)", "Option Value", false);
        }
        if(greeksButtons[6].isSelected())
        {
            for(int i = 0; i < 500; i++)
            {
                ys[i] = round(scale * i * Math.exp(-1 * q * t) * PDF(d1StockPrice(i)) * Math.sqrt(t) * d1StockPrice(i) * d2StockPrice(i) / iv, 4);
                p.addPoint(i, h - (int)(1.5 * ys[i]));
            }
            pcHelper(g1, g2, w, h, "Stock Price", "Vomma", false);
        }
        if(greeksButtons[7].isSelected()) {
            for(int i = 0; i < 500; i++)
            {
                ys[i] = round(((i * Math.exp(-1 * q * t) * CNDF(d1StockPrice(i)) - k * Math.exp(-1 * r * t) * CNDF(d2StockPrice(i))) * scale), 4);
                p.addPoint(i, h - (int)(1.5 * ys[i]));
            }
            pcHelper(g1, g2, w, h, "Stock Price", "Option Value", false);
        }
        if(greeksButtons[8].isSelected()) {
            for(int i = 0; i < 500; i++)
            {
                ys[i] = round(scale * i * Math.exp(-1 * q * t) * PDF(d1StockPrice(i)) * Math.sqrt(t), 4);
                p.addPoint(i, h - (int)(1.5 * ys[i]));
            }
            pcHelper(g1, g2, w, h, "Stock Price", "Vega", false);
        }
        if(greeksButtons[9].isSelected()) {
            for(int i = 0; i < 500; i++)
            {
                double t1 = q * Math.exp(-1 * q * t) * CNDF(d1StockPrice(i));
                double t2 = Math.exp(-1 * q * t) * PDF(d1StockPrice(i));
                double t3 = (2 * (r - q) * t - d2StockPrice(i) * iv * Math.sqrt(t)) / (2 * t * iv * Math.sqrt(t));
                ys[i] = round(scale * (t1 - (t2 * t3)) / 365.0, 8);
                p.addPoint(i, h - (int)(1.5 * ys[i]));
            }
            pcHelper(g1, g2, w, h, "Stock Price", "Charm", true);
        }
        if(greeksButtons[10].isSelected()) {
            for(int i = 0; i < 500; i++)
            {
                ys[i] = round(scale * -1 * Math.exp(-1 * q * t) * PDF(d1StockPrice(i)) * d2StockPrice(i) / iv, 5);
                p.addPoint(i, h - (int)(1.5 * ys[i]));
            }
            pcHelper(g1, g2, w, h, "Stock Price", "Vanna", true);
        }
        if(greeksButtons[11].isSelected()) {
            for(int i = 0; i < 500; i++)
            {
                ys[i] = round(scale * i * Math.exp(-1 * q * t) * CNDF(d1StockPrice(i)) / (i * Math.exp(-1 * q * t) * CNDF(d1StockPrice(i)) - k * Math.exp(-1 * r * t) * CNDF(d2StockPrice(i))), 5);
                p.addPoint(i, h - (int)(1.5 * ys[i]));
            }
            pcHelper(g1, g2, w, h , "Stock Price", "Lambda", false);
        }

        if(!isNewGraph && !greeksButtons[0].isSelected() && mouseX - 8 < 501 && mouseY - 30 < 701)
            g1.drawLine(mouseX-8, mouseY-30, mouseX-8, 330 - (int) (1.5 * ys[mouseX - 8]) );
    }
    public static void main(String[] args) {
        initFrame();
    }

    public static void restoreDefaultsActionPerformed(ActionEvent e) {
        defaultS = 200.0;
        k = 200.0;
        r = .03;
        q = .01;
        iv = .6;
        t = .05;
        sliders[0].setValue(200);
        sliders[1].setValue(200);
        sliders[2].setValue(60);
        sliders[3].setValue((int) (.05 * 365));
        sliders[4].setValue((int) (.03 * 10000));
        sliders[5].setValue((int) (.01 * 10000));
        updateGraph(false);
    }

    public static void frameMouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        if(e.getX() < 509) {
            if(!l.isVisible())
                l.setVisible(true);

            l.setLocation(e.getX(), e.getY());

            if(greeksButtons[0].isSelected())
                l.setText("<html>Stock Price: " + (e.getX() - 8) + "<br/>Delta: " + (ys[e.getX() - 8] / scale) + "</html>");
            else if(greeksButtons[1].isSelected())
                l.setText("<html>Stock Price: " + (e.getX() - 8) + "<br/>Gamma: " + (ys[e.getX() - 8] / scale) + "</html>");
            else if(greeksButtons[2].isSelected())
                l.setText("<html>Stock Price: " + (e.getX() - 8) + "<br/>Theta: " + (ys[e.getX() - 8] / scale) + "</html>");
            else if(greeksButtons[3].isSelected())
                l.setText("<html>IV: " + (e.getX() - 8) + "<br/>Vega: " + (ys[e.getX() - 8] / scale) + "</html>");
            else if(greeksButtons[4].isSelected())
                l.setText("<html>DTE: " + (e.getX() - 8) + "<br/>Premium: " + (ys[e.getX() - 8] / scale) + "</html>");
            else if(greeksButtons[5].isSelected())
                l.setText("<html>IV: " + (e.getX() - 8) + "<br/>Value: " + (ys[e.getX() - 8] / scale) + "</html>");
            else if(greeksButtons[6].isSelected())
                l.setText("<html>Stock Price: " + (e.getX() - 8) + "<br/>Vomma: " + (ys[e.getX() - 8] / scale) + "</html>");
            else if(greeksButtons[7].isSelected())
                l.setText("<html>Stock Price: " + (e.getX() - 8) + "<br/>Option Value: " + (ys[e.getX() - 8] / scale) + "</html>");
            else if(greeksButtons[8].isSelected())
                l.setText("<html>Stock Price: " + (e.getX() - 8) + "<br/>Vega: " + (ys[e.getX() - 8] / scale) + "</html>");
            else if(greeksButtons[9].isSelected())
                l.setText("<html>Stock Price: " + (e.getX() - 8) + "<br/>Charm: " + (ys[e.getX() - 8] / scale) + "</html>");
            else if(greeksButtons[10].isSelected())
                l.setText("<html>Stock Price: " + (e.getX() - 8) + "<br/>Vanna: " + (ys[e.getX() - 8] / scale) + "</html>");
            else if(greeksButtons[11].isSelected())
                l.setText("<html>Stock Price: " + (e.getX() - 8) + "<br/>Lambda: " + (ys[e.getX() - 8] / scale) + "</html>");
        }
        else
            l.setVisible(false);

        updateGraph(false);
    }

    public static void frameMouseWheelMoved(MouseWheelEvent e) {
        double zoomAmt = Math.pow(1.1, e.getPreciseWheelRotation());
        scale *= zoomAmt;
        opc.repaint();
    }

    public static void sliderValues1ActionPerformed(MouseEvent ignoredE){
        String newSP = JOptionPane.showInputDialog(f, "Enter the new stock price");
        sliderValues[0].setText(newSP);
        sliders[0].setValue(Integer.parseInt(newSP));
        defaultS = Double.parseDouble(newSP);
        optionVars[0] = defaultS;
        updateOptionVars();
        updateGraph(false);
    }

    public static void sliderValues2ActionPerformed(MouseEvent ignoredE){
        String newK = JOptionPane.showInputDialog(f, "Enter the new strike price");
        sliderValues[1].setText(newK);
        sliders[1].setValue(Integer.parseInt(newK));
        defaultS = Double.parseDouble(newK);
        optionVars[1] = defaultS;
        updateOptionVars();
        updateGraph(false);
    }
}



