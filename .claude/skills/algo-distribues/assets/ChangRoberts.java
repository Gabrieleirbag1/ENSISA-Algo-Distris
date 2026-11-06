package ensisa;

import ensisa.process.InputMessage;
import ensisa.process.OutputMessage;
import ensisa.process.Process;

import java.io.IOException;
import java.util.Random;

public class ChangRoberts extends Process {
    static final int CANDIDATE = 0;
    static final int ELECTED = 1;

    public ChangRoberts() throws IOException {
    }

    public static void main(String[] args) throws Exception {
        var process = new ChangRoberts();
        process.start();
    }

    public void run() throws Exception {
        int candidate = new Random().nextInt(1000) * 100 + id;
        int next = outNeighbors.get(0);
        int leader = -1;

        System.out.println("Processus " + id + " propose " + candidate);
        send(next, new OutputMessage(CANDIDATE, candidate));

        while (leader == -1) {
            InputMessage message = receive();
            int value = message.readInt();
            if (message.type == CANDIDATE) {
                if (value > candidate) {
                    send(next, new OutputMessage(CANDIDATE, value));
                } else if (value == candidate) {
                    send(next, new OutputMessage(ELECTED, id));
                }
            } else {
                leader = value;
                if (leader != id) {
                    send(next, new OutputMessage(ELECTED, leader));
                }
            }
        }

        System.out.println("Processus " + id + " : l'élu est " + leader);
    }
}
