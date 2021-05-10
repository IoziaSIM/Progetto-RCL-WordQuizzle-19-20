import java.util.Timer;
import java.util.TimerTask;

//timer della sfida
public class TimerCh {
    private Timer timer;
    private Challenge chall;

    public TimerCh(int sec, Challenge chall) {
        this.chall = chall;
        timer = new Timer();
        timer.schedule(new RemindTask(), sec * 1000);
    }

    class RemindTask extends TimerTask {
        public void run() {
            if (chall.isAlive())
                chall.timeout.incrementAndGet();
        }
    }
}