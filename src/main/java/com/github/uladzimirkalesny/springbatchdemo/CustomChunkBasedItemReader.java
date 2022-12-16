package com.github.uladzimirkalesny.springbatchdemo;

import org.springframework.batch.item.ItemReader;

import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

public class CustomChunkBasedItemReader implements ItemReader<String> {

    private final List<String> items;
    private final Iterator<String> iterator;

    public CustomChunkBasedItemReader() {
        this.items = IntStream.rangeClosed(1, 5)
                .mapToObj(Integer::toString)
                .toList();
        this.iterator = this.items.iterator();
    }

    /**
     * This is method is going to be called over and over again
     * (once per each item found in the chunks that are read from data set when the job is processing),
     * basically until it returns null signaling to the framework
     * that it has exhausted the items from the datasource.
     */
    @Override
    public String read() {
        return iterator.hasNext() ? iterator.next() : null;
    }

}
