package com.Carewell.ecg700;
import java.lang.reflect.Array;

/***
 * 数组顺序队列,用于缓存波形数据
 * @param <T>  
 */
public class Fifo<T> {
	private T[] buf;
	private int rear; //对尾
	public int front; //对头
	public int maxSize;//队列最大长度

	@SuppressWarnings("unchecked")
	public Fifo(Class<T> classType, int length){
    	buf=(T[])Array.newInstance(classType, length);
		rear = 0;
		front = 0;
		maxSize = length;
    }  
	
	public void clear() {
		rear = 0;
		front = 0;
	}
	
	/** 返回队列的元素个数，及队列长度。 （现有元素数量） */
    public int size() {
        return (rear - front + maxSize) % maxSize;
    }
    
    public int offset() {
    	return rear;
    }
    
    /** 队列剩余空间 */
    public int space() {
    	return maxSize - size() - 1;
    }
    
    /** 判断是否满,0:满，1：空  */
    public int isFull() {
    	if((rear +1)% maxSize == front)
    		return 0;
    	else
    		return 1;
    }
    
    /** 取位置相对值 */
    public T get(int num) {
        return buf[(front + num) % maxSize];
    }
    
    /** 取位置绝对值 */
    public T getAbs(int num) {
    	num = num % maxSize;
    	return buf[num];
    }

    public int getRear() {
    	return rear;
    }
	
    /** 更改相对位置值  */
    public int set(int num, T value) {
        if (num >= this.size())
            return -1;
        buf[(front + num) % maxSize] = value;
        return 0;
    }
    
    /**  插入一个数据  */
    public int push(T data) {
    	if(buf == null || isFull()==0)
    		return -1;
		  buf[rear] = data; //对尾插入
		  rear = (rear + 1) % maxSize;
		  return 0;
    }

    /** 插入一批数据 */
    public int ens(T[] data_buf, int offset, int data_len) {
        if (maxSize - this.size() < data_len)
            return -1;
        for (int i = 0; i < data_len; i++)
        {
            buf[rear] = data_buf[offset + i];
            rear = (rear + 1) % maxSize;
        }
        return 0;
    }

    /**  取出一个数据  */
    public T pop() {
    	T data;
    	
        if (rear != front)
        {
            data = buf[front];
            front = (front + 1) % maxSize;
            return data;
        }
        else
            return null;
    }

    /**  取出一批数据  */
    public int des(T[] data_buf, int offset, int data_len) {
        if (this.size() < data_len)
            return -1;
        for (int i = 0; i < data_len; i++)
        {
            data_buf[offset + i] = buf[front];
            front = (front + 1) % maxSize;
        }
        return 0;
    }
    
    /** 只读，不移除 */
    public int gets(T[] data_buf, int offset, int data_len) {
    	int peer = front;
    	
    	if (this.size() < data_len)
            return -1;
        for (int i = 0; i < data_len; i++)
        {
            data_buf[offset + i] = buf[peer];
            peer = (peer + 1) % maxSize;
        }
        return 0;
    }
    
//    public int read(T[] data_buf, int offset, int data_len)
//    {
//    	int len = (this.size() < data_len)?this.size():data_len;
//    	
//        for (int i = 0; i < len; i++)
//        {
//            data_buf[i] = buf[(front + offset + i)%maxSize];
//        }
//        return len;
//    }

}
