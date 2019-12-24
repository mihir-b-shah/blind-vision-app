import serial
from matplotlib import pyplot as plt
import keyboard

ard = serial.Serial('com5', 9600)
line = "empty"
ctr = 0;
xs = []
ys = []

time = ""
plt.grid(True, which='both')

while(line != ""):
    if(keyboard.is_pressed('q')):
       break
    line = str(ard.readline())
    ctr += 1
    print(line[2:len(line)-5])
    # process
    if(ctr % 2 == 0):
        xs.append(int(time[2:len(time)-5]))
        ys.append(int(line[2:len(line)-5]))
        plt.scatter(int(time[2:len(time)-5]),int(line[2:len(line)-5]),5,color='blue')
        plt.pause(0.000001)
    else:
        time = line

plt.close()
plt.plot(xs,ys)
plt.show()
