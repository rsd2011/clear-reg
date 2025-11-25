package com.example.server.notice;

import com.example.common.jpa.PrimaryKeyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "notice_sequences")
public class NoticeSequence extends PrimaryKeyEntity {

    @Column(name = "sequence_year", nullable = false, unique = true)
    private Integer sequenceYear;

    @Column(name = "next_value", nullable = false)
    private Integer nextValue = 1;

    @Version
    private long version;

    protected NoticeSequence() {
    }

    public NoticeSequence(Integer sequenceYear) {
        this.sequenceYear = sequenceYear;
    }

    public Integer getSequenceYear() {
        return sequenceYear;
    }

    public Integer getNextValue() {
        return nextValue;
    }

    public long getVersion() {
        return version;
    }

    public int next() {
        int value = nextValue;
        nextValue = nextValue + 1;
        return value;
    }
}
