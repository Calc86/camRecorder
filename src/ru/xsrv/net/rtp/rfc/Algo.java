package ru.xsrv.net.rtp.rfc;

import ru.xsrv.net.utils.BIT;
import ru.xsrv.net.utils.uInt;
import ru.xsrv.net.utils.uShort;

/**
 * Created by calc on 29.07.14.
 *
 */
public class Algo {
    public static void init_seq(source s, short seq){
        s.base_seq = BIT.uShort(seq) - 1;
        s.max_seq = seq;
        s.bad_seq = Constants.RTP_SEQ_MOD + 1;
        s.cycles = 0;
        s.received = 0;
        s.received_prior = 0;
        s.expected_prior = 0;
    }

    public static int update_seq(source s, short seq){
        short udelta = uShort.sub(seq, s.max_seq);
        final int MAX_DROPOUT = 3000;
        final int MAX_MISORDER = 100;
        final int MIN_SEQUENTIAL = 2;

        /*
        * Source is not valid until MIN_SEQUENTIAL packets with
        * sequential sequence numbers have been received.
        */
        if (s.probation != 0) {
           /* packet is in sequence */
            if (seq == uShort.add(s.max_seq, (short)1)) {
                s.probation--;
                s.max_seq = seq;
                if (s.probation == 0) {
                    init_seq(s, seq);
                    s.received++;
                    return 1;
                }
            } else {
                s.probation = MIN_SEQUENTIAL - 1;
                s.max_seq = seq;
            }
            return 0;
        } else if (udelta < MAX_DROPOUT) {
           /* in order, with permissible gap */
            if (uShort.get(seq) < uShort.get(s.max_seq)) {
               /*
               * Sequence number wrapped - count another 64K cycle.
                */
                s.cycles += Constants.RTP_SEQ_MOD;
            }
            s.max_seq = seq;
        } else if (uShort.get(udelta) <= Constants.RTP_SEQ_MOD - MAX_MISORDER) {
           /* the sequence number made a very large jump */
            if (seq == s.bad_seq) {
               /*
                * Two sequential packets -- assume that the other side
                * restarted without telling us so just re-sync
                * (i.e., pretend this was the first packet).
                */
                init_seq(s, seq);
            }
            else {
                s.bad_seq = (uShort.add(seq, (short)1)) & (Constants.RTP_SEQ_MOD - 1);
                return 0;
            }
        } else {
           /* duplicate or reordered packet */
        }
        s.received++;
        return 1;
    }

    public void A2RTCPHeaderValidityChecks(rtcp_t r, int len){
        //int len;        /* length of compound RTCP packet in words */ //can get from DatagramPacket or MagicFrame
        //rtcp_t r;          /* RTCP header */
        //rtcp_t end;        /* end of compound RTCP packet */

        if ((r.common.getHeader()[0] & Constants.RTCP_VALID_MASK) != Constants.RTCP_VALID_VALUE) {
           /* something wrong with packet format */
        }
        //end = (rtcp_t *)((u_int32 *)r + len);

        int i = r.common.length();
        /*do r = (rtcp_t *)((u_int32 *)r + r->common.length + 1);
        while (r < end && r->common.version == 2);

        if (r != end) {
           /* something wrong with packet format */
        //}
    }

    public static void A3DeterminingTheNumberOfRTPPacketsExpectedAndLost(source s){
        int extended_max = s.cycles + uShort.get(s.max_seq);
        int expected = extended_max - s.base_seq + 1;
        int lost = expected - s.received;

        System.out.println("lost: " + lost);
        System.out.println("expected: " + expected);

        int expected_interval = expected - s.expected_prior;
        s.expected_prior = expected;
        int received_interval = s.received - s.received_prior;
        s.received_prior = s.received;
        int lost_interval = expected_interval - received_interval;
        int fraction;
        if (expected_interval == 0 || lost_interval <= 0) fraction = 0;
        else fraction = (lost_interval << 8) / expected_interval;

        System.out.println("fraction: " + fraction);
    }

    public static double rtcp_interval(int members,
                         int senders,
                         double rtcp_bw,
                         int we_sent,
                         int packet_size,
                         Integer avg_rtcp_size, //*
                         int initial)
    {
       /*
        * Minimum time between RTCP packets from this site (in seconds).
        * This time prevents the reports from `clumping' when sessions
        * are small and the law of large numbers isn't helping to smooth
        * out the traffic.  It also keeps the report interval from
        * becoming ridiculously small during transient outages like a
        * network partition.
        */
        final double RTCP_MIN_TIME = 5.;
       /*
        * Fraction of the RTCP bandwidth to be shared among active
        * senders.  (This fraction was chosen so that in a typical
        * session with one or two active senders, the computed report
        * time would be roughly equal to the minimum report time so that
        * we don't unnecessarily slow down receiver reports.) The
        * receiver fraction must be 1 - the sender fraction.
        */
        final double RTCP_SENDER_BW_FRACTION = 0.25;
        final double RTCP_RCVR_BW_FRACTION = (1-RTCP_SENDER_BW_FRACTION);
       /*
        * Gain (smoothing constant) for the low-pass filter that
       * estimates the average RTCP packet size (see Cadzow reference).
        */
        final double RTCP_SIZE_GAIN = (1./16.);

        double t;                   /* interval */
        double rtcp_min_time = RTCP_MIN_TIME;
        int n;                      /* no. of members for computation */

       /*
        * Very first call at application start-up uses half the min
        * delay for quicker notification while still allowing some time
        * before reporting for randomization and to learn about other
        * sources so the report interval will converge to the correct
        * interval more quickly.  The average RTCP size is initialized
        * to 128 octets which is conservative (it assumes everyone else
        * is generating SRs instead of RRs: 20 IP + 8 UDP + 52 SR + 48
        * SDES CNAME).
        */
        if (initial != 0) {
            rtcp_min_time /= 2;
            avg_rtcp_size = 128;
        }

       /*
        * If there were active senders, give them at least a minimum
        * share of the RTCP bandwidth.  Otherwise all participants share
        * the RTCP bandwidth equally.
        */
        n = members;
        if (senders > 0 && senders < members * RTCP_SENDER_BW_FRACTION) {
            if (we_sent != 0) {
                rtcp_bw *= RTCP_SENDER_BW_FRACTION;
                n = senders;
            } else {
                rtcp_bw *= RTCP_RCVR_BW_FRACTION;
                n -= senders;
            }
        }

       /*
        * Update the average size estimate by the size of the report
        * packet we just sent.
        */
        avg_rtcp_size += (int)((packet_size - avg_rtcp_size)*RTCP_SIZE_GAIN);

       /*
        * The effective number of sites times the average packet size is
        * the total number of octets sent when each site sends a report.
         * Dividing this by the effective bandwidth gives the time
        * interval over which those packets must be sent in order to
        * meet the bandwidth target, with a minimum enforced.  In that
        * time interval we send one report so this time is also our
        * average time between reports.
        */
        t = avg_rtcp_size * n / rtcp_bw;
        if (t < rtcp_min_time) t = rtcp_min_time;

       /*
        * To avoid traffic bursts from unintended synchronization with
        * other sites, we then pick our actual next report interval as a
        * random number uniformly distributed between 0.5*t and 1.5*t.
        */
        return t * (Math.random() + 0.5);
    }

    public void A8EstimatinTheInterarrivalJitter(rtp_hdr_t r, rtcp_rr_t rr, source s){
        long arrival = System.currentTimeMillis() / 1000L;

        int transit = (int)(arrival - uInt.get(r.ts()));
        int d = (int)((long)transit - uInt.get(s.transit));
        s.transit = transit;
        if (d < 0) d = -d;
        s.jitter += (1./16.) * ((double)d - s.jitter);
        //rr.jitter() = s.jitter;
        //s.jitter += d - ((s.jitter + 8) >> 4);
        //!!rr.jitter = s->jitter >> 4;
    }

    public static void A8EstimatinTheInterarrivalJitter2(long ts, source s){
        long arrival = System.currentTimeMillis() / 1000L;

        int transit = (int)(arrival - ts);
        int d = (int)((long)transit - uInt.get(s.transit));
        s.transit = transit;
        if (d < 0) d = -d;
        System.out.println((1./16.) * ((double)d - s.jitter));
        s.jitter += (1./16.) * ((double)d - s.jitter);
        //rr.jitter() = s.jitter;
        //s.jitter += d - ((s.jitter + 8) >> 4);
        //!!rr.jitter = s->jitter >> 4;
    }

    public static void test1(){
        source s = new source();
        s.probation = 3;

        //short[] seq_s = {13547, 13548, 13549, 13551, 13552, 13553, 13589};
        short[] seq_s = {(short)65534, (short)65535, 0, 1, 3, 4, 5};

        for(short seq: seq_s){
            update_seq(s, seq);
            A3DeterminingTheNumberOfRTPPacketsExpectedAndLost(s);
            long arrival = System.currentTimeMillis() / 1000L;
            A8EstimatinTheInterarrivalJitter2(arrival, s);
            System.out.println(s);
        }

        System.out.println("line");
    }

    public static void test2(){
        source s = new source();
        long[] sleeps = {3000, 2000};

        for(long sleep : sleeps){
            long arrival = System.currentTimeMillis() / 1000L;
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            A8EstimatinTheInterarrivalJitter2(arrival, s);
            System.out.println(s);
        }
    }

    public static void main(String[] args) {
        //System.out.println(rtcp_interval(1, 1, 3000000, 0, 0, 0, 0));
    }
}
