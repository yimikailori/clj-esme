package javacompile;


public class JenkinsHash {
    private static final long MAX_VALUE = 4294967295L;
    long a;
    long b;
    long c;

    public JenkinsHash() {
    }

    private long byteToLong(byte b) {
        long val = (long)(b & 127);
        if ((b & 128) != 0) {
            val += 128L;
        }

        return val;
    }

    private long add(long val, long add) {
        return val + add & 4294967295L;
    }

    private long subtract(long val, long subtract) {
        return val - subtract & 4294967295L;
    }

    private long xor(long val, long xor) {
        return (val ^ xor) & 4294967295L;
    }

    private static long leftShift(long val, int shift) {
        return val << shift & 4294967295L;
    }

    private long fourByteToLong(byte[] bytes, int offset) {
        return this.byteToLong(bytes[offset + 0]) + (this.byteToLong(bytes[offset + 1]) << 8) + (this.byteToLong(bytes[offset + 2]) << 16) + (this.byteToLong(bytes[offset + 3]) << 24);
    }

    private void hashMix() {
        this.a = this.subtract(this.a, this.b);
        this.a = this.subtract(this.a, this.c);
        this.a = this.xor(this.a, this.c >> 13);
        this.b = this.subtract(this.b, this.c);
        this.b = this.subtract(this.b, this.a);
        this.b = this.xor(this.b, leftShift(this.a, 8));
        this.c = this.subtract(this.c, this.a);
        this.c = this.subtract(this.c, this.b);
        this.c = this.xor(this.c, this.b >> 13);
        this.a = this.subtract(this.a, this.b);
        this.a = this.subtract(this.a, this.c);
        this.a = this.xor(this.a, this.c >> 12);
        this.b = this.subtract(this.b, this.c);
        this.b = this.subtract(this.b, this.a);
        this.b = this.xor(this.b, leftShift(this.a, 16));
        this.c = this.subtract(this.c, this.a);
        this.c = this.subtract(this.c, this.b);
        this.c = this.xor(this.c, this.b >> 5);
        this.a = this.subtract(this.a, this.b);
        this.a = this.subtract(this.a, this.c);
        this.a = this.xor(this.a, this.c >> 3);
        this.b = this.subtract(this.b, this.c);
        this.b = this.subtract(this.b, this.a);
        this.b = this.xor(this.b, leftShift(this.a, 10));
        this.c = this.subtract(this.c, this.a);
        this.c = this.subtract(this.c, this.b);
        this.c = this.xor(this.c, this.b >> 15);
    }

    public long hash(byte[] buffer, long initialValue) {
        this.a = 2654435769L;
        this.b = 2654435769L;
        this.c = initialValue;
        int pos = 0;

        int len;
        for(len = buffer.length; len >= 12; len -= 12) {
            this.a = this.add(this.a, this.fourByteToLong(buffer, pos));
            this.b = this.add(this.b, this.fourByteToLong(buffer, pos + 4));
            this.c = this.add(this.c, this.fourByteToLong(buffer, pos + 8));
            this.hashMix();
            pos += 12;
        }

        this.c += (long)buffer.length;
        switch(len) {
            case 11:
                this.c = this.add(this.c, leftShift(this.byteToLong(buffer[pos + 10]), 24));
            case 10:
                this.c = this.add(this.c, leftShift(this.byteToLong(buffer[pos + 9]), 16));
            case 9:
                this.c = this.add(this.c, leftShift(this.byteToLong(buffer[pos + 8]), 8));
            case 8:
                this.b = this.add(this.b, leftShift(this.byteToLong(buffer[pos + 7]), 24));
            case 7:
                this.b = this.add(this.b, leftShift(this.byteToLong(buffer[pos + 6]), 16));
            case 6:
                this.b = this.add(this.b, leftShift(this.byteToLong(buffer[pos + 5]), 8));
            case 5:
                this.b = this.add(this.b, this.byteToLong(buffer[pos + 4]));
            case 4:
                this.a = this.add(this.a, leftShift(this.byteToLong(buffer[pos + 3]), 24));
            case 3:
                this.a = this.add(this.a, leftShift(this.byteToLong(buffer[pos + 2]), 16));
            case 2:
                this.a = this.add(this.a, leftShift(this.byteToLong(buffer[pos + 1]), 8));
            case 1:
                this.a = this.add(this.a, this.byteToLong(buffer[pos + 0]));
            default:
                this.hashMix();
                return this.c;
        }
    }

    public long hash(byte[] buffer) {
        return this.hash(buffer, 0L);
    }
}
