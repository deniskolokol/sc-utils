Server.default = s = Server.internal;

// Set this up according to the sound card specs.
s.options.numInputBusChannels = 4;
s.options.numOutputBusChannels = 10;

s.options.blockSize = 512;

// 2Gb
s.options.memSize = 2097152;
s.options.numBuffers = 2048;
s.boot;
