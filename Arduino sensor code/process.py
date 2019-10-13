
from matplotlib import pyplot as plt
from scipy import signal as sgn
from scipy.stats import norm
import seaborn

u = 0
v = 0;

def main():
    f = open("C:\\Users\\mihir\\Documents\\Arduino projects\\data7.txt", "r")
    ctr = 0
    num_lines=0
    start = 0
    time_vals = []
    time_vals_2 = []
    signal_vals = []
    line_past = ""

    for line in f:
        if(line_past == line):
            continue
        index = line.index('>')-2
        time_loc = line[0:index]
        val = int(line[index+3:])
        arr = time_loc.split(':')
        ind2 = arr[2].index('.')
        millis = int(arr[2][ind2+1:])
        secs = int(arr[2][0:ind2])
        mins = int(arr[1])
        hrs = int(arr[0])
        time = millis+1000*(secs + 60*(mins+ 60*hrs))
        if(ctr == 0):
            start = time
        time_vals.append(time-start)
        signal_vals.append(val)
        ctr+=1
        line_past = line
        num_lines += 1

    # Process the data by removing the repeated t-values.
    
    limit = len(time_vals)
    i = 0
    j = 0
    while(i < limit):
        j = i
        while(j < limit-1 and (time_vals[j] == time_vals[j+1])):
            j += 1
        if(i < j-1):
            dt = (time_vals[j-1]-time_vals[i])/(j-1-i)
            for k in range(i,j-1):
                time_vals[k+1] = time_vals[k] + dt
        i = j+1
    
    latency = (time_vals[-1]-time_vals[0])/num_lines
    for k in range(0, len(signal_vals)):
        time_vals_2.append(latency*k)
    
    it = 0
    while(time_vals[it] == time_vals[it+1]):
          it += 1

    print(latency)
    noise_train(time_vals_2, signal_vals, 655)
    color_vals = []
    #seaborn.set()
    #ax = seaborn.distplot(signal_vals)
    #plt.show()
    print(noise_compute(0.99, signal_vals, 768, 824))
    graph(time_vals_2, signal_vals)

def graph(time_vals, signal_vals):
    #s-g filter
    plt.plot(time_vals,signal_vals)
    plt.scatter(time_vals,signal_vals,5)
    plt.grid(True, which='both')
    plt.xlim(8200, 8800)
    plt.ylim(105, 115)
    plt.show()

def fst_deriv(signal_vals, index):
    return signal_vals[index]-signal_vals[index-1]

def noise_train(time_vals_2, signal_vals, end):
    global u
    global v
    u = 0
    v = 0
    for i in range(1, end):
        u = (u*(i-1) + abs(signal_vals[i] - signal_vals[i-1]))/i
        v = (v*(i-1) + (abs(signal_vals[i]-signal_vals[i-1])-u)**2)/i
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
        p = norm.cdf((abs(extr)-u)/s,u,s)
        if(p > k):
            sn = 0 if(extr == 0) else abs(extr)/extr 
            extr = 0
            if(sn != compl):
                compl = sn
                ct += 1
                print()
            
    return ct/2
