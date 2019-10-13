
from matplotlib import pyplot as plt
from scipy import signal as sgn
from scipy.stats import norm
import seaborn

u = 0
v = 0
MAX = 0

def main():
    f = open("C:\\Users\\mihir\\Documents\\Arduino projects\\data8.txt", "r")
    time_vals_2 = []
    signal_vals = []
    control_vals = []
    ctr = 0

    for line in f:
        if(ctr % 2 == 0):
            signal_vals.append(int(line))
        if(ctr % 2 == 1):
            control_vals.append(int(line))
        ctr+=1
    
    #latency = (time_vals[-1]-time_vals[0])/num_lines NEED TO CHANGE
    latency = 10
    for k in range(0, len(signal_vals)):
        time_vals_2.append(latency*k)

    noise_train(time_vals_2, control_vals)
    #seaborn.set()
    #ax = seaborn.distplot(signal_vals)
    #plt.show()
    print(noise_compute(0.99, signal_vals, 1170, 1210))
    graph(time_vals_2, signal_vals, control_vals)

def graph(time_vals, signal_vals, control_vals):
    #s-g filter
    plt.plot(time_vals,signal_vals)
    plt.plot(time_vals,control_vals)
    plt.scatter(time_vals,signal_vals,5)
    plt.scatter(time_vals,control_vals,5)
    plt.grid(True, which='both')
    plt.xlim(0, time_vals[-1])
    plt.show()

def fst_deriv(signal_vals, index):
    return signal_vals[index]-signal_vals[index-1]

def noise_train(time_vals_2, signal_vals):
    global u
    global v
    global MAX
    u = 0
    v = 0
    for i in range(1, len(signal_vals)):
        u = (u*(i-1) + abs(signal_vals[i] - signal_vals[i-1]))/i
        v = (v*(i-1) + (abs(signal_vals[i]-signal_vals[i-1])-u)**2)/i
        MAX = max(MAX, abs(signal_vals[i]-signal_vals[i-1]))
    print("%f %f" %(u,v))
    
# need to tweak for data input
def noise_compute(k, signal_vals, st, end):
    global u
    global v
    ct = 0
    s = v**0.5
    # p is the fraction of all noise smaller than this interval. use a cdf.
    extr = signal_vals[st-1]
    sn = 0 # 1 is - and 2 is +
    compl = 0 # 1 is False and 2 is True
    for i in range(st,end):
        print(signal_vals[i],end=' ')
        extr += signal_vals[i]-signal_vals[i-1]
        #p = norm.cdf((abs(extr)-u)/s,u,s)
        #if(p > k):
        sn = 0 if(extr == 0) else abs(extr)/extr 
        extr = 0
        if(sn != compl):
            compl = sn
            ct += 1
            print()        
    return ct/2
