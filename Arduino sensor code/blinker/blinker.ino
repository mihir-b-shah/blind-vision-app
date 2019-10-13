bool state;

void setup()
{
  pinMode(8, OUTPUT);
  state = false;
}

void loop()
{
    digitalWrite(8, state ? HIGH : LOW);
    state = !state;
    delay(1000);
}
