 
const int ph1 = 0;
const int ph2 = 1;
int lightLevel = 0;

void setup()
{
  Serial.begin(9600);
  delay(25);
}

void loop()
{
    //if(Serial.available()) 
    Serial.println(analogRead(ph1));
    Serial.println(analogRead(ph2));
    //Serial.println("Hi!");
}
