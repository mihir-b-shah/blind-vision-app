package com.apps.navai;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.physicaloid.lib.Physicaloid;

public class ArduinoInterface extends IntentService implements DataStream.SpeedListener {

    public static final String INPUT_FILTER = "string_9430493102";
    public static final String MESSAGE_KEY = "message_439403";
    public static final String MESSAGE_DONE = "message_done030";
    public static final String MESSAGE_CHANGE = "message_chg3902";

    public static final String DIR_X = "dirx_40394011";
    public static final String DIR_Y = "diry_90697069";

    private Physicaloid phy;
    private final ByteArray buf;
    private final DataStream stream;

    // meters
    private static final double WHEEL_RADIUS = 0.064;
    private static final double WALKER_DIAMETER = 0.445;

    class CartVector {
        private double x;
        private double y;

        public CartVector(double x, double y) {
            this.x = x; this.y = y;
        }
    }

    private final CartVector displ;
    private final CartVector dir;

    private double velocity1;
    private double velocity2;
    private double time1;
    private double time2;

    @Override
    public void speedChanged(int id, double time) {
        double w = Math.PI/time;
        double interval;

        if (id == 0) {
            velocity1 = w*WHEEL_RADIUS;
            time1 += time;
            interval = time1-time2;
        } else {
            velocity2 = w*WHEEL_RADIUS;
            time2 += time;
            interval = time2-time1;
        }

        double mgn = interval*(velocity1+velocity2)/2;
        displ.x += mgn*dir.x;
        displ.y += mgn*dir.y;

        double angRot = interval*(velocity2-velocity1)/WALKER_DIAMETER;
        final double cosAR = Math.cos(angRot); final double sinAR = Math.sin(angRot);
        dir.x = cosAR*dir.x + sinAR*dir.y;
        dir.y = -sinAR*dir.x + cosAR*dir.y;

        Intent intent = new Intent(Navigate.XY_FILTER);
        intent.putExtra("X", displ.x);
        intent.putExtra("Y", displ.y);
        intent.putExtra(DIR_X, dir.x);
        intent.putExtra(DIR_Y, dir.y);
        sendBroadcast(intent);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(INPUT_FILTER)) {
                String message = intent.getStringExtra(MESSAGE_KEY);
                switch(message) {
                    case MESSAGE_DONE:
                        close();
                        stopSelf();
                        break;
                    case MESSAGE_CHANGE:
                        byte[] chgKey = {-1,0,0};
                        stream.enqueue(chgKey, chgKey.length);
                        break;
                    default:
                        System.err.println("Wrong message received.");
                }
            }
        }
    };

    public ArduinoInterface() {
        super("ArduinoInterface");
        stream = new DataStream(0.99f, this); // set
        buf = new ByteArray(12); // grows by expand()
        displ = new CartVector(0,0);
        dir = new CartVector(0, 1);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        registerReceiver(receiver, new IntentFilter(INPUT_FILTER));

        phy = new Physicaloid(this);
        phy.setBaudrate(9600);

        if(phy.open()) {
            phy.addReadListener(i -> {
                buf.expand(i);
                phy.read(buf.getBuffer(), i);
                Log.v("Buffer val", buf.limString(i));
                stream.enqueue(buf.getBuffer(), i);
            });
        }
    }

    private void close() {
        if(phy.close())
            phy.clearReadListener();
    }
}
