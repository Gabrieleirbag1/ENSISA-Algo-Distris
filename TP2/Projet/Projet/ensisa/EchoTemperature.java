package ensisa;

import ensisa.process.InputMessage;
import ensisa.process.OutputMessage;
import ensisa.process.Process;

import java.io.IOException;
import java.util.Random;

public class EchoTemperature extends Process {
    static final int WAVE = 0;
    static final int ECHO = 1;

    public EchoTemperature() throws IOException {
    }

    public static void main(String[] args) throws Exception {
        var process = new EchoTemperature();
        process.start();
    }

    public void run() throws Exception {
        int temperature = 10 + new Random().nextInt(21);
        int sum = temperature;
        int sensors = 1;
        int father = -1;
        int count = 0;
        System.out.println("Processus " + id + " : température " + temperature);

        if (id != 0) {
            father = receive().senderProcessId;
            count++;
        }

        for (int neighbor : outNeighbors) {
            if (neighbor != father) {
                send(neighbor, new OutputMessage(WAVE));
            }
        }

        while (count < inNeighbors.size()) {
            InputMessage message = receive();
            if (message.type == ECHO) {
                sum += message.readInt();
                sensors += message.readInt();
            }
            count++;
        }

        if (id == 0) {
            double average = (double) sum / sensors;
            System.out.println("Processus 0 : température moyenne " + average + " (" + sum + " / " + sensors + ")");
        } else {
            OutputMessage echo = new OutputMessage(ECHO);
            echo.writeInt(sum);
            echo.writeInt(sensors);
            send(father, echo);
        }
    }
}
