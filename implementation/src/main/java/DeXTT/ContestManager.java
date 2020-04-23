package DeXTT;

import DeXTT.DataStructure.DeXTTAddress;
import DeXTT.DataStructure.PoITimeHash;
import DeXTT.DataStructure.ProofOfIntentFull;
import DeXTT.DataStructure.VetoFinalizeData;
import Events.NewBlockFoundEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.time.Instant;
import java.util.*;

public class ContestManager {

    private static final Logger logger = LogManager.getLogger();

    private BigInteger biggerThanMaxHash;
    private DeXTTAddress biggestDeXTTAddress;

    private Map<BigInteger, ProofOfIntentFull> contestsParticipated; // key: poihash, value: poi

    private SortedSet<PoITimeHash> endTimes;
    private SortedSet<VetoFinalizeData> endTimesVeto;

    private Map<String, Date> latestBlockTimes; // key: rpc-url of client,
    private String earliestBlockChain; // rpc Url of chain with earliest latest block time
    private Date latestKnownBlockTime;

    // used to handle contest participation
    private Map<BigInteger, List<DeXTTAddress>> contestParticipants; // key: poihash, value: list of all addresses which already participated in contest
    private Map<BigInteger, ProofOfIntentFull> contestsStarted;

    public ContestManager() {
        this.contestsParticipated = new HashMap<>();

        this.latestKnownBlockTime = Date.from(Instant.EPOCH);

        this.endTimes = new TreeSet<>();
        this.endTimesVeto = new TreeSet<>();

        this.latestBlockTimes = new HashMap<>();
        this.earliestBlockChain = null;

        this.contestsParticipated = new HashMap<>();

        this.contestParticipants = new HashMap<>();
        this.contestsStarted = new HashMap<>();

        byte[] biggerThanMaxHash = new byte[33]; // 33 bytes, first one is 1; => bigger than biggest possible hash (32 bytes)
        biggerThanMaxHash[0] = 0x01;
        for (int i = 1; i < biggerThanMaxHash.length; i++) {
            biggerThanMaxHash[i] = 0x00;
        }
        this.biggerThanMaxHash = new BigInteger(1, biggerThanMaxHash);
        this.biggestDeXTTAddress = new DeXTTAddress("ffffffffffffffffffffffffffffffffffffffff");
    }

    public void addContestParticipation(BigInteger poiHash, ProofOfIntentFull poi) {
        this.contestsParticipated.put(poiHash, poi);
    }

    public boolean participatedIn(BigInteger poiHash) {
        return this.contestsParticipated.containsKey(poiHash);
    }

    public ProofOfIntentFull getParticipatedPoi(BigInteger poiHash) {
        return this.contestsParticipated.get(poiHash);
    }

    public Date getEarliestBlockTime() {
        if (this.earliestBlockChain != null) {
            return this.latestBlockTimes.getOrDefault(this.earliestBlockChain, Date.from(Instant.EPOCH));
        } else {
            return Date.from(Instant.EPOCH);
        }
    }

    public void setNewBlockTime(NewBlockFoundEvent event) {
        this.latestBlockTimes.put(event.getRpcUrl(), event.getBlockTime());

        if (this.earliestBlockChain == null || this.earliestBlockChain.equals(event.getRpcUrl())) { // was earliest chain until now -> reevaluate earliest chain
            // iterate over all time -> find smallest
            Map.Entry<String, Date> earliestEntry = null;
            for (Map.Entry<String, Date> entry: this.latestBlockTimes.entrySet()) {
                if (earliestEntry == null || entry.getValue().before(earliestEntry.getValue())) {
                    earliestEntry = entry;
                }
            }
            this.earliestBlockChain = earliestEntry.getKey();
        }

        if (this.getLatestKnownBlockTime().before( event.getBlockTime())) {
            this.latestKnownBlockTime = event.getBlockTime();
        }
    }

    public String getLatestProcessedChain() {
        Map.Entry<String, Date> latestEntry = null;
        for (Map.Entry<String, Date> entry: this.latestBlockTimes.entrySet()) {
            if (latestEntry == null || entry.getValue().after(latestEntry.getValue())) {
                latestEntry = entry;
            }
        }
        if (latestEntry != null) {
            return latestEntry.getKey();
        } else {
            return null;
        }
    }

    public Date getLatestProcessedBlockTime() {
        Map.Entry<String, Date> latestEntry = null;
        for (Map.Entry<String, Date> entry: this.latestBlockTimes.entrySet()) {
            if (latestEntry == null || entry.getValue().after(latestEntry.getValue())) {
                latestEntry = entry;
            }
        }
        if (latestEntry != null) {
            return latestEntry.getValue();
        } else {
            return null;
        }
    }

    public synchronized Date getLatestKnownBlockTime() {
        return latestKnownBlockTime;
    }

    public synchronized void setLatestKnownBlockTime(Date latestKnownBlockTime) {
        this.latestKnownBlockTime = latestKnownBlockTime;
    }

    public synchronized boolean isAfterBlockChainTime(Date date) {
        return date.after(this.latestKnownBlockTime);
    }

    public void addPoiEndTime(Date endTime, BigInteger poiHash) {
        this.endTimes.add(new PoITimeHash(endTime, poiHash));
    }

    public SortedSet<PoITimeHash> getPoisEndedBefore(Date time) {
        SortedSet<PoITimeHash> ready = this.endTimes.headSet(new PoITimeHash(time, this.biggerThanMaxHash)); // includes PoITimeHash with same time, because hash < biggerThanMaxHash
        return ready;
    }

    public void deletePoiEndTime(PoITimeHash poITimeHash) {
        this.endTimes.remove(poITimeHash);
    }

    public void addVetoEndTime(Date endTime, DeXTTAddress conflictingSender) {
        this.endTimesVeto.add(new VetoFinalizeData(endTime, conflictingSender));
    }

    public SortedSet<VetoFinalizeData> getVetoEndedBefore(Date time) {
        SortedSet<VetoFinalizeData> ready = this.endTimesVeto.headSet(new VetoFinalizeData(time, biggestDeXTTAddress)); // includes Veto with same time, because address <= biggestAddress
        return ready;
    }

    public void deleteVetoFinalizeDate(VetoFinalizeData vetoFinalizeData) {
        this.endTimesVeto.remove(vetoFinalizeData);
    }

    public void addStartedContest(BigInteger poiHash, ProofOfIntentFull poiFull) {
        this.contestsStarted.put(poiHash, poiFull);
    }

    public boolean hasContestStarted(BigInteger poiHash) {
        return this.contestsStarted.containsKey(poiHash);
    }

    public Map<BigInteger, ProofOfIntentFull> getStartedContests() {
        return this.contestsStarted;
    }

    public synchronized void addContestParticipant(BigInteger poiHash, DeXTTAddress participant) {
        this.contestParticipants.compute(poiHash, (k,v) -> {
            if (v == null) {
                v = new ArrayList<>();
            }
            if (!v.contains(participant)) {
                v.add(participant);
            }
            return v;
        });
    }

    public synchronized boolean isContestParticipant(BigInteger poiHash, DeXTTAddress participant) {
        List<DeXTTAddress> participants = this.contestParticipants.get(poiHash);
        if (participants != null) {
            return participants.contains(participant);
        } else {
            return false;
        }
    }

    /**
     *
     * @param poiHash
     * @return      best contest participation signature
     *              0, if no participation yet
     *              null, if contest has not yet started
     */
    public synchronized BigInteger getBestContestParticipation(BigInteger poiHash) {
        List<DeXTTAddress> participants = this.contestParticipants.get(poiHash);
        if (participants != null) {
            BigInteger currentBest = BigInteger.valueOf(0);
            for (DeXTTAddress participant: participants) {
                BigInteger signature = Cryptography.contestantSignature(participant, poiHash);
                if (currentBest.compareTo(signature) < 0) {
                    currentBest = signature;
                }
            }
            return currentBest;
        } else {
            return null;
        }
    }
}
