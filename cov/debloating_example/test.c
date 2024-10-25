int main(int argc, char** argv) {
  int x = atoi(*(argv + 1));
  if (x < 0) {
    printf("Less than 0.\n");
  }
  else if (x == 0) {
    printf("Equal to 0.\n");
  }
  else {
    printf("Greater than 0.\n");
  }
}
