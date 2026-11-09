package ensisa;

import ensisa.process.InputMessage;
import ensisa.process.OutputMessage;
import ensisa.process.Process;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

public class EchoCollect extends Process {
    static final int WAVE = 0;

    public EchoCollect() throws IOException {
    }

    public static void main(String[] args) throws Exception {
        var process = new EchoCollect();
        process.start();
    }

    public void run() throws Exception {
        String text = "Bonjour à tous";
        Set<Integer> known = new TreeSet<>();
        int father = -1;
        int count = 0;

        if (id != 0) {
            InputMessage first = receive();
            father = first.senderProcessId;
            text = first.readString();
            known.addAll(readIds(first));
            known.add(id);
            count++;
            System.out.println("Processus " + id + " a reçu \"" + text + "\" de " + father);
        }

        OutputMessage wave = createMessage(text, known);
        for (int neighbor : outNeighbors) {
            if (neighbor != father) {
                send(neighbor, wave);
            }
        }

        while (count < inNeighbors.size()) {
            InputMessage message = receive();
            message.readString();
            Set<Integer> ids = readIds(message);
            known.addAll(ids);
            count++;
            if (id == 0) {
                System.out.println("Processus 0 a reçu de " + message.senderProcessId + " : " + ids);
            }
        }

        if (id == 0) {
            System.out.println("Processus 0 : diffusion terminée");
            System.out.println("Processus 0 connaît : " + known);
        } else {
            send(father, createMessage(text, known));
        }
    }

    OutputMessage createMessage(String text, Set<Integer> ids) {
        OutputMessage message = new OutputMessage(WAVE);
        message.writeString(text);
        message.writeInt(ids.size());
        for (int i : ids) {
            message.writeInt(i);
        }
        return message;
    }

    Set<Integer> readIds(InputMessage message) {
        Set<Integer> ids = new TreeSet<>();
        int size = message.readInt();
        for (int i = 0; i < size; i++) {
            ids.add(message.readInt());
        }
        return ids;
    }
}
