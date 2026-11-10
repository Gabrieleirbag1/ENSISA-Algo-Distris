package ensisa;

import ensisa.process.InputMessage;
import ensisa.process.OutputMessage;
import ensisa.process.Process;

import java.io.IOException;
import java.util.ArrayList;

import static java.lang.System.exit;


public class TestMesh1 extends Process {
    /**
     * Constructor
     *
     * @throws IOException
     */
    public TestMesh1() throws IOException {
    }

    public static void main(String[] args) throws Exception {
        var process = new TestMesh1();
        process.start();
    }

    // Mesh test
    public void run() throws Exception {
        System.out.println("Process " + id + " started");

        if (!inNeighbors.containsAll(outNeighbors) || !outNeighbors.containsAll(inNeighbors)) {
            System.out.println("inNeighbors and outNeighbors differ\n");
        }

        // From process 0 to all others
        if (id == 0) {
            for (Integer outNeighbor : outNeighbors) send(outNeighbor, new OutputMessage(MessageType.None.value, id));

            ArrayList<Integer> outN = new ArrayList<>(outNeighbors);
            for (int n = 0; n < outNeighbors.size(); n++) {
                InputMessage message = receive();
                if (!outN.contains(message.senderProcessId)) {
                    String msg = "Process 0 received message from incorrect process " + message.senderProcessId + "\n";
                    System.out.println(msg);
                    exit(1);
                }
                outN.remove((Integer) message.senderProcessId);
            }
            if (outN.size() > 0) {
                String msg = "Process 0 did not receive message from process " + outN.get(0) + "\n";
                System.out.println(msg);
                exit(1);
            }
        } else {
            InputMessage message = receiveFrom(0);
            int v = message.readInt();
            if (v != 0) {
                String msg = "Process " + id + " received incorrect message\n";
                System.out.println(msg);
                exit(1);
            }
            send(0, new OutputMessage(MessageType.None.value, v));
            String msg = "Process " + id + " received message from " + 0 + " and replied\n";
            System.out.println(msg);
        }
    }

    enum MessageType {
        None(0);

        final int value;

        MessageType(int value) {
            this.value = value;
        }
    }
}

