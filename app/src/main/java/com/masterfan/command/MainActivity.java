package com.masterfan.command;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private static final int ADB_START_BEGIN = 0x01;
    private static final int ADB_START_END = 0x02;
    private static final int ADB_STOP_BEGIN = 0x03;
    private static final int ADB_STOP_END = 0x04;

    private final String START_COMMAND = "setprop service.adb.tcp.port 5555";
    private final String STOP_COMMAND = "setprop service.adb.tcp.port -1";

    private TextView resultTxt;
    private EditText edit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        edit = (EditText) findViewById(R.id.input_edit);
        resultTxt = (TextView) findViewById(R.id.resutl_txt);

        edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    String commandStr = v.getText().toString();
                    if (TextUtils.isEmpty(commandStr))
                        return false;

                    if (commandStr.equals(START_COMMAND))
                        adbStart();
                    else if (commandStr.equals(STOP_COMMAND))
                        adbStop();
                    else if(commandStr.equalsIgnoreCase("exit")) {
                        edit.setEnabled(false);
                        resultTxt.setText(resultTxt.getText().toString() + "\n" + "Bye Bye ..." + "\n ");
                        edit.setText("");
                        handler.sendEmptyMessageDelayed(10, 1000);
                    }else
                        doPerform(commandStr);
                }
                return false;
            }
        });

        handler.sendEmptyMessageDelayed(0, 1500);
    }

    private void adbStart() {
        handler.sendEmptyMessage(ADB_START_BEGIN);
        runRootCommand(START_COMMAND);
        try {
            if (isProcessRunning("adbd")) ;
            runRootCommand("stop adbd");
            runRootCommand("start adbd");
        } catch (Exception e) {
            e.printStackTrace();
        }
        handler.sendEmptyMessage(ADB_START_END);
    }

    private void adbStop() {
        handler.sendEmptyMessage(ADB_STOP_BEGIN);
        runRootCommand(STOP_COMMAND);
        try {
            runRootCommand("stop adbd");
            runRootCommand("start adbd");
        } catch (Exception e) {
            e.printStackTrace();
        }
        handler.sendEmptyMessage(ADB_STOP_END);
    }

    public boolean isProcessRunning(String processName) throws Exception {
        boolean running = false;
        Process process = null;
        process = Runtime.getRuntime().exec("ps");
        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = null;
        while ((line = in.readLine()) != null) {
            if (line.contains(processName)) {
                running = true;
                break;
            }
        }
        in.close();
        process.waitFor();
        return running;
    }

    public boolean runRootCommand(String command) {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
            }
        }
        return true;
    }

    private void doPerform(String command) {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process proc = runtime.exec(command);
            InputStream inputstream = proc.getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
            // read the ls output
            String line = "";
            StringBuilder sb = new StringBuilder(line);
            while ((line = bufferedreader.readLine()) != null) {
                //System.out.println(line);
                sb.append(line);
                sb.append('\n');
            }
            bufferedreader.close();
            resultTxt.setText(resultTxt.getText().toString() + command + "\n" + sb.toString() + "\n ");
            edit.setText("");
            //使用exec执行不会等执行成功以后才返回,它会立即返回
            //所以在某些情况下是很要命的(比如复制文件的时候)
            //使用wairFor()可以等待命令执行完成以后才返回
            try {
                if (proc.waitFor() != 0) {
                    resultTxt.setText(resultTxt.getText().toString() + "exit value = " + proc.exitValue() + "\n");
                }
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    resultTxt.setText("MasterFan terminal for android\n(c) 2015 [版本1.0.1]\n\n ");
                    break;
                case ADB_START_BEGIN:
                    edit.setEnabled(false);
                    resultTxt.setText(resultTxt.getText().toString() + START_COMMAND + "\n" + "processing ..." + "\n ");
                    edit.setText("");
                    break;
                case ADB_START_END:
                    resultTxt.setText(resultTxt.getText().toString() + "processing success!" + "\n\n ");
                    edit.setEnabled(true);
                    break;
                case ADB_STOP_BEGIN:
                    edit.setEnabled(false);
                    resultTxt.setText(resultTxt.getText().toString() + STOP_COMMAND + "\n" + "processing ..." + "\n ");
                    edit.setText("");
                    break;
                case ADB_STOP_END:
                    edit.setEnabled(true);
                    resultTxt.setText(resultTxt.getText().toString() + "processing success!" + "\n\n ");
                    break;
                case 10:
                    finish();
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
