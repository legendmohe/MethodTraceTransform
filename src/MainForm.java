import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainForm {
    private JPanel main_form;
    private JTextPane tp_cmd_output;
    private JButton btn_run;
    private JButton btn_reset;
    private JTextArea ta_input;
    private JTextField tf_thread_name;

    private SequenceDiagramsProcessor processor;

    public static void main(String[] args) {
        JFrame frame = new JFrame("MainForm");
        MainForm mainForm = new MainForm();
        frame.setContentPane(mainForm.main_form);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        mainForm.init();
    }

    private void init() {
        processor = new SequenceDiagramsProcessor();
        initActions();
    }

    private void initActions() {
        btn_reset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reset();
            }
        });
        btn_run.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String output = processor.process(ta_input.getText(), tf_thread_name.getText());
                tp_cmd_output.setText(output);
            }
        });
    }

    ////////////////////////////////private///////////////////////////////////////////

    private void runCmd(final String cmd, final RunCmdCallback callback) {
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    ProcessBuilder builder = new ProcessBuilder(
                            cmd.split("\\s+"));
                    builder.redirectErrorStream(true);
                    Process p = builder.start();
                    BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    while (true) {
                        String line = r.readLine();
                        if (line == null) {
                            break;
                        }
                        if (callback != null) {
                            callback.onFetchLine(line);
                        }
                    }
                    if (callback != null) {
                        callback.onComplete(null);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (callback != null) {
                        callback.onComplete(e);
                    }
                }
            }
        });
        thread.start();
    }

    private void reset() {
        ta_input.setText("");
        tp_cmd_output.setText("");
    }

    private interface RunCmdCallback {
        void onFetchLine(String line);

        void onComplete(Exception e);
    }
}
