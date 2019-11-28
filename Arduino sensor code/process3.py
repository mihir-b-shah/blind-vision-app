from matplotlib import pyplot as plt
import rainflow

def scale(array,factor):
    minval = min(array)
    maxval = max(array)
    center = (minval+maxval)/2
    stdev = center-minval
    for i in range(0, len(array)):
        array[i] = (array[i]-center)*factor+center;

def shift(array,val):
    for i in range(0, len(array)):
        array[i] += val

def superimpose(array1,array2):
    min1 = min(array1)
    max1 = max(array1)
    min2 = min(array2)
    max2 = max(array2)

    spread1 = abs(max1-min1)
    spread2 = abs(max2-min2)

    scl = spread2/spread1
    print(scl)
        
    center1 = (min1+max1)/2
    center2 = (min2+max2)/2
    
    scale(array1,scl)
    shift(array2,center1-center2)

def analyze(file):
    lines = file.readlines()

    xs = []
    ys = []
    
    for i in range(0, len(lines)//2):
        xs.append(int(lines[2*i])-int(lines[0]))
        ys.append(int(lines[2*i+1]))

    return xs,ys

f = open("dataWITHLIGHT.txt","r")
g = open("dataWITHOUTLIGHT.txt","r")

x1,y1 = analyze(f)
x2,y2 = analyze(g)

ct = rainflow.extract_cycles(y2)
for tupl in ct:
    print(tupl)

#superimpose(y1,y2)

plt.plot(x1,y1,10)
plt.plot(x2,y2,10)
plt.grid(True, which='both')
plt.xlim(0, min(x1[-1],x2[-1]))
plt.show()       
