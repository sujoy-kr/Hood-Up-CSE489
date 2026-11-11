package com.example.carworkshopbookingapp.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AdvisorReply {
    public enum Urgency { LOW, MEDIUM, HIGH }
    public final String reply;
    public final Urgency urgency;
    public final List<String> serviceIds;
    public final boolean offline;

    public AdvisorReply(String reply, Urgency urgency, List<String> ids, boolean offline) {
        this.reply = reply; this.urgency = urgency;
        serviceIds = Collections.unmodifiableList(new ArrayList<>(ids)); this.offline = offline;
    }
}
