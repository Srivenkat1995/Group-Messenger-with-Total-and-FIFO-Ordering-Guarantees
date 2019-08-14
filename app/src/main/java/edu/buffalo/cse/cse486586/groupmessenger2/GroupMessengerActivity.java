package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import static java.lang.Integer.parseInt;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */

public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final int[] REMOTE_PORT = {11108,11112,11116,11120,11124};
    static final int SERVER_PORT = 10000;
    public static final String ENTRY_MESSAGE = "Entry_Message";
    public static final String PROPOSED_MESSAGE = "Proposed_Message";
    public static final String DELIVER_MESSAGE = "Deliver_Message";

    int sequenceDelivery = 0;
    double sequenceProposed = 0;
    int counter = 0 ;
    int failed_port = 0;
    int port = 0;
    Comparator<Data> comparator = new Comparator<Data>() {
        @Override
        public int compare(Data lhs, Data rhs) {
            if(lhs.max_agreed > rhs.max_agreed){ return 1;}
            else {return -1;}
        }
    };
    PriorityQueue<Data> Queue = new PriorityQueue<Data>(25, comparator);




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */

        // Reference from PA1

        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portString = telephonyManager.getLine1Number().substring(telephonyManager.getLine1Number().length() - 4);
        final String myPort = String.valueOf((parseInt(portString) * 2 ));

        try
        {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            Log.e(TAG, "onCreate: Server Socket Created");
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        }
        catch(IOException e)
        {
            Log.e(TAG, "onCreate: Socket Creation Failed ");
        }


        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */



        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button button = (Button) findViewById(R.id.button4);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: Button OnClick Listener Instantiated ");
                String message = editText.getText().toString() + "\n";
                editText.setText("");
                TextView textView = (TextView) findViewById(R.id.textView1);
                textView.append("\t" + message);
                Data dataObject;
                dataObject = intialiseData(message, myPort);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, dataObject);


            }
        });


    }

    private Data intialiseData(String message, String myPort) {

        Data dataObject = new Data();
        dataObject.setType(ENTRY_MESSAGE);
        dataObject.setMsg(message);
        dataObject.setCount(counter);
        counter = counter + 1 ;
        dataObject.setSender_port(parseInt(myPort));
        return dataObject;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void>{

        // https://developer.android.com/reference/java/io/DataInputStream.html
        // https://developer.android.com/reference/java/io/DataOutputStream.html


        @Override
        protected Void doInBackground(ServerSocket... serverSockets) {

            ServerSocket serverSocket = serverSockets[0];
            Log.e(TAG, "doInBackground: ServerTask " + serverSocket);
            try{

                while(true)
                {
                    Log.e(TAG, "doInBackground: ServerTask Instantiated");
                    Socket socket = serverSocket.accept();
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    Log.e(TAG, "doInBackground: inputStream" + inputStream);
                    ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                    Log.e(TAG, "doInBackground: outputStream" + outputStream );
                    Data data = (Data) inputStream.readObject();
                    Log.e(TAG, "doInBackground:Data" + data.msg);
                    if(data.getType().equals(ENTRY_MESSAGE))
                    {
                        data.setType(PROPOSED_MESSAGE);
                        data.setSuggested_seq(sequenceProposed);
                        sequenceProposed = sequenceProposed + ((int) (Math.random() * 100 + 1));
                        data.setDeliverable(false);
                        Queue.add(data);
                        outputStream.writeObject(data);
                        outputStream.flush();

                    }

                    if(data.getFailed_port() != 0)
                    {
                        Iterator<Data> iterator = Queue.iterator();
                        while (iterator.hasNext())
                        {
                            Data current = iterator.next();
                            if(current.getSender_port() == data.failed_port && current.isDeliverable() == false)
                            {
                                Queue.remove(current);

                            }
                        }
                    }

                    if(data.getType().equals(DELIVER_MESSAGE))
                    {
                        if(data.getMax_agreed() >= sequenceProposed)
                        {
                            sequenceProposed = data.getMax_agreed() + 1;
                        }
                        Iterator<Data> iterator = Queue.iterator();
                        while(iterator.hasNext()){
                            Data current = iterator.next();
                            if(data.getMsg().equals(current.msg))
                            {
                                Queue.remove(current);

                            }
                        }
                        data.setDeliverable(true);
                        Queue.add(data);
                        Data acknowledgement = new Data();
                        acknowledgement.setType("ack");
                        outputStream.writeObject(acknowledgement);
                        outputStream.flush();
                        Log.e(TAG, "doInBackground: Queue" + Queue);
                        while (Queue.peek() != null)
                        {
                            if(Queue.peek().deliverable)
                            {
                                Data msg = Queue.poll();
                                publishProgress(msg.getMsg());
                                Uri uri = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider");
                                ContentValues contentValues = new ContentValues();
                                contentValues.put("key",sequenceDelivery);
                                contentValues.put("value",msg.msg);
                                getContentResolver().insert(uri,contentValues);
                                sequenceDelivery++;
                                Log.e(TAG, "onProgressUpdate: KEY,VALUE Pair appended");
                            }
                            else
                            {
                                break;
                            }
                        }

                    }

                    inputStream.close();
                    outputStream.close();
                    socket.close();

                }
            }
            catch (IOException e)
            {
                Log.e(TAG, "Input Exceptions" + port);
            }
            catch (ClassNotFoundException e)
            {
                Log.e(TAG, "doInBackground: Class NOt Found");
            }

            
            
            



            return  null;

        }

        protected void onProgressUpdate(String... values) {

            String received = values[0].trim();
            TextView textView = (TextView) findViewById(R.id.textView1);
            textView.append(received + "\t\n");

            return;

        }
    }


    private class ClientTask extends AsyncTask<Data, Void, Void>{

        @Override
        protected Void doInBackground(Data... datas) {

            Data msg = datas[0];
            Log.e(TAG, "doInBackground: Data"  + msg.type);
            if (msg.getType().equals(ENTRY_MESSAGE))
            {
                try {
                    double max = 0;
                    Data proposed_data;
                    Data[] proposed_datas = new Data[5];
                    Socket socket;
                    ObjectOutputStream outputStreamClientTask;
                    ObjectInputStream inputStreamClientTask;

                    for (int i = 0; i < REMOTE_PORT.length; i++) {
                        Log.e(TAG, "doInBackground: FOR loop"+ REMOTE_PORT[i] );
                        if (failed_port != REMOTE_PORT[i]) {
                            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    REMOTE_PORT[i]);
                            socket.setSoTimeout(500);
                            Log.e(TAG, "doInBackground: Socket in Client Task " + socket);
                            try {
                                Log.e(TAG, "doInBackground: Output Stream Client Task");
                                outputStreamClientTask = new ObjectOutputStream(socket.getOutputStream());
                                Log.e(TAG, "doInBackground: Input Stream Client Task");
                                inputStreamClientTask = new ObjectInputStream(socket.getInputStream());
                            }
                            catch (IOException e)
                            {
                                failed_port = REMOTE_PORT[i];
                                socket.close();
                                continue;
                            }


                            port = REMOTE_PORT[i];
                            msg.setSuggester_port(REMOTE_PORT[i]);
                            msg.setFailed_port(failed_port);
                            Log.e(TAG, "doInBackground: Before Output Stream");
                            outputStreamClientTask.writeObject(msg);
                            outputStreamClientTask.flush();
                            Log.e(TAG, "doInBackground: After Output Stream");
                            Log.e(TAG, "doInBackground: Inside For Loop");
                            proposed_data = (Data) inputStreamClientTask.readObject();
                            Log.e(TAG, "doInBackground: " + proposed_data);
                            switch (proposed_data.getSuggester_port()) {
                                case 11108:
                                    proposed_datas[0] = proposed_data;
                                    break;
                                case 11112:
                                    proposed_datas[1] = proposed_data;
                                    break;
                                case 11116:
                                    proposed_datas[2] = proposed_data;
                                    break;
                                case 11120:
                                    proposed_datas[3] = proposed_data;
                                    break;
                                case 11124:
                                    proposed_datas[4] = proposed_data;
                                    break;
                            }

                            if (proposed_data.getType().equals(PROPOSED_MESSAGE)) {
                                if (proposed_data.getSuggested_seq() > max) {
                                    max = proposed_data.getSuggested_seq();
                                }
                            }

                            if (proposed_data != null) {
                                outputStreamClientTask.close();
                                inputStreamClientTask.close();
                                socket.close();
                            }

                        }
                    }
                    Socket newSocket;
                    Data acknowledgement = null;
                    ObjectInputStream inputStream1;
                    ObjectOutputStream outputStream1;
                    for (int j = 0; j < REMOTE_PORT.length; j++) {
                        if (failed_port != REMOTE_PORT[j]) {
                            newSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    REMOTE_PORT[j]);
                            try {
                                outputStream1 = new ObjectOutputStream(newSocket.getOutputStream());
                                inputStream1 = new ObjectInputStream(newSocket.getInputStream());
                            } catch (IOException e) {
                                failed_port = REMOTE_PORT[j];
                                newSocket.close();
                                continue;
                            }
                            port = REMOTE_PORT[j];

                            if (proposed_datas[j].getType().equals(PROPOSED_MESSAGE)) {

                                proposed_datas[j].setMax_agreed(max);
                                proposed_datas[j].setType(DELIVER_MESSAGE);
                                proposed_datas[j].setFailed_port(failed_port);
                                outputStream1.writeObject(proposed_datas[j]);
                                outputStream1.flush();
                            }
                            try {
                                acknowledgement = (Data) inputStream1.readObject();

                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                            if (acknowledgement.type.equals("ack")) {
                                //Log.v(TAG,"Releasing all the resources");
                                //Release resources
                                outputStream1.close();
                                inputStream1.close();
                                newSocket.close();
                            }


                        }
                    }
                }
                catch (UnknownHostException e)
                {
                    Log.e(TAG, "doInBackground: UnKnownHost Exception");

                }
                catch (IOException e)
                {
                    Log.e(TAG, "doInBackground: InputOutput Exception");
                    failed_port = port;
                    Log.e(TAG,"pORT" + failed_port + "has crashed");
                }
                catch (ClassNotFoundException e)
                {
                    Log.e(TAG, "doInBackground: Class Not Found Exception");
                }

            }











            return null;
        }
    }

}
