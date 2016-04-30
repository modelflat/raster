package raster;

import java.util.ArrayList;

class ChunkPool {
	
	private ArrayList<Integer> chunks;
	
	ChunkPool(int count) {
		this.chunks = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
			this.chunks.add(i);
	}
	
	synchronized int freeCount() {
		return this.chunks.size();
	}
	
	synchronized int get() {
		if (this.chunks.size() > 0)
			return this.chunks.remove(0);
		return -1;
	}
	
}
