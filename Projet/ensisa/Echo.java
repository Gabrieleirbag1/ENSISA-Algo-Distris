package ensisa;

import ensisa.process.InputMessage;
import ensisa.process.OutputMessage;
import ensisa.process.Process;

import java.io.IOException;

public class Echo extends Process {
    static final int WAVE = 0;

    public Echo() throws IOException {
    }

    public static void main(String[] args) throws Exception {
        var process = new Echo();
        process.start();
    }

    public void run() throws Exception {
        String text = "Bonjour à tous";
        int father = -1;
        int count = 0;

        if (id != 0) {
            InputMessage first = receive();
            father = first.senderProcessId;
            text = first.readString();
            count++;
            System.out.println("Processus " + id + " a reçu \"" + text + "\" de " + father);
        }

        OutputMessage message = new OutputMessage(WAVE);
        message.writeString(text);

        for (int neighbor : outNeighbors) {
            if (neighbor != father) {
                send(neighbor, message);
            }
        }

        while (count < inNeighbors.size()) {
            receive();
            count++;
        }

        if (id == 0) {
            System.out.println("Processus 0 : diffusion terminée");
        } else {
            send(father, message);
        }
    }
}
