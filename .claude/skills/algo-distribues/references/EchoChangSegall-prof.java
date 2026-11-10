/**
 * Philippe STUDER
 * © ENSISA 2023
 */

package ensisa;

import ensisa.process.InputMessage;
import ensisa.process.OutputMessage;
import ensisa.process.Process;

import java.io.IOException;
import java.util.HashSet;

import static java.lang.System.exit;

public class EchoChangSegall extends Process {
    int rec = 0;
    int father = -1;

    /**
     * Constructor
     *
     * @throws IOException
     */
    public EchoChangSegall() throws IOException {
    }

    public static void main(String[] args) throws Exception {
        var process = new EchoChangSegall();
        process.start();
    }

    @Override
    protected void run() throws Exception {
        if (id == 0) {
            // Initiator
            OutputMessage message = new OutputMessage(MessageType.None.value);
            message.writeString("Hello from ");

            for (var p : outNeighbors) {
                send(p, message);
            }
            while (rec < inNeighbors.size()) {
                InputMessage msg = receive();
                rec++;
            }

            System.out.println("All processes visited");
        } else {
            // Follower
            InputMessage msg = receive();
            String s = msg.readString();

            System.out.println(s + id);

            father = msg.senderProcessId;
            rec = rec + 1;

            OutputMessage message = new OutputMessage(MessageType.None.value);
            message.writeString(s);

            for (var p : outNeighbors) {
                if (p != father)
                    send(p, message);
            }
            while (rec < inNeighbors.size()) {
                msg = receive();
                rec++;
            }
            message = new OutputMessage(MessageType.None.value);
            message.writeString(s);
            send(father, message);
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
