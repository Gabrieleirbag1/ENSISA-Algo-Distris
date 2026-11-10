package ensisa;

import ensisa.process.InputMessage;
import ensisa.process.OutputMessage;
import ensisa.process.Process;

import java.io.IOException;
import java.util.Random;

public class EchoChangSegall extends Process {
    static final int WAVE = 0;
    static final int ECHO = 1;
    static final int AVERAGE = 2;

    public EchoChangSegall() throws IOException {
    }

    public static void main(String[] args) throws Exception {
        var process = new EchoChangSegall();
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

        double average;
        count = 0;
        if (id == 0) {
            average = (double) sum / sensors;
            System.out.println("Processus 0 : température moyenne " + average + " (" + sum + " / " + sensors + ")");
        } else {
            OutputMessage echo = new OutputMessage(ECHO);
            echo.writeInt(sum);
            echo.writeInt(sensors);
            send(father, echo);

            InputMessage first = receive();
            father = first.senderProcessId;
            average = first.stream().readDouble();
            count++;
            System.out.println("Processus " + id + " : température " + temperature + ", moyenne reçue " + average);
        }

        OutputMessage result = new OutputMessage(AVERAGE);
        result.stream().writeDouble(average);
        for (int neighbor : outNeighbors) {
            if (neighbor != father) {
                send(neighbor, result);
            }
        }

        while (count < inNeighbors.size()) {
            receive();
            count++;
        }

        if (id == 0) {
            System.out.println("Processus 0 : diffusion de la moyenne terminée");
        } else {
            send(father, result);
        }
    }
}
