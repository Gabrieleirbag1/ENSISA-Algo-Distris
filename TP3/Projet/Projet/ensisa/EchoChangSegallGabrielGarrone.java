package ensisa;

import ensisa.process.InputMessage;
import ensisa.process.OutputMessage;
import ensisa.process.Process;

import java.io.IOException;
import java.util.Random;

public class EchoChangSegallGabrielGarrone extends Process {
    static final int WAVE = 0;
    static final int ECHO = 1;
    static final int RESULT = 2;

    public EchoChangSegallGabrielGarrone() throws IOException {
    }

    public static void main(String[] args) throws Exception {
        var process = new EchoChangSegallGabrielGarrone();
        process.start();
    }

    public void run() throws Exception {
        int temperature = new Random().nextInt(41);
        int min = temperature;
        int max = temperature;
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
                min = Math.min(min, message.readInt());
                max = Math.max(max, message.readInt());
                sensors += message.readInt();
            }
            count++;
        }

        count = 0;
        if (id == 0) {
            System.out.println("Processus 0 : température minimale " + min + ", maximale " + max + " (" + sensors + " capteurs)");
        } else {
            OutputMessage echo = new OutputMessage(ECHO);
            echo.writeInt(min);
            echo.writeInt(max);
            echo.writeInt(sensors);
            send(father, echo);

            InputMessage first = receive();
            father = first.senderProcessId;
            min = first.readInt();
            max = first.readInt();
            count++;
            System.out.println("Processus " + id + " : température " + temperature + ", minimale reçue " + min + ", maximale reçue " + max);
        }

        OutputMessage result = new OutputMessage(RESULT);
        result.writeInt(min);
        result.writeInt(max);
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
            System.out.println("Processus 0 : diffusion terminée");
        } else {
            send(father, result);
        }
    }
}
