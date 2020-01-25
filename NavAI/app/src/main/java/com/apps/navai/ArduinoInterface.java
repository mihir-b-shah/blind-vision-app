package com.apps.navai;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.physicaloid.lib.Physicaloid;

public class ArduinoInterface extends IntentService implements DataStream.SpeedListener {

    private Physicaloid phy;
    private final ByteArray buf;
    private final DataStream stream;

    private static double X;
    private static double Y;

    // computed as cos/sin of 2*pi*wheel_radius/walker_diameter
    // nonsense values
    private static final double COS_ROT = 0.97;
    private static final double SIN_ROT = 0.04;
    private static final double WALKER_DIAMETER = 1.02;

    public ArduinoInterface() {
        super("ArduinoInterface");
        stream = new DataStream(0.99f, this); // set
        buf = new ByteArray(12); // grows by expand()
    }

    @Override
    protected void onHandleIntent(Intent intent) {
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

    @Override
    public void speedChanged(int id, double time) {
        if(id == 0) {
            X += WALKER_DIAMETER*(1-COS_ROT);
        } else {
            X -= WALKER_DIAMETER*(1-COS_ROT);
        }
        Y += WALKER_DIAMETER*SIN_ROT;
        Intent intent = new Intent(Navigate.XY_FILTER);
        intent.putExtra("X", X);
        intent.putExtra("Y", Y);
        sendBroadcast(intent);
    }
}
