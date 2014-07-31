package com.net.rtp.rfc;

/**
 * Created by calc on 29.07.14.
 *
 */
public class source {
    public short max_seq;        /* highest seq. number seen */
    public int cycles;         /* shifted count of seq. number cycles */
    public int base_seq;       /* base seq number */
    public int bad_seq;        /* last 'bad' seq number + 1 */
    public int probation;      /* sequ. packets till source is valid */
    public int received;       /* packets received */
    public int expected_prior; /* packet expected at last interval */
    public int received_prior; /* packet received at last interval */
    public int transit;        /* relative trans time for prev pkt */
    public int jitter;         /* estimated jitter */

    @Override
    public String toString() {
        return "source{" +
                "\n max_seq=" + max_seq +
                "\n cycles=" + cycles +
                "\n base_seq=" + base_seq +
                "\n bad_seq=" + bad_seq +
                "\n probation=" + probation +
                "\n received=" + received +
                "\n expected_prior=" + expected_prior +
                "\n received_prior=" + received_prior +
                "\n transit=" + transit +
                "\n jitter=" + jitter +
                "\n}";
    }
}
