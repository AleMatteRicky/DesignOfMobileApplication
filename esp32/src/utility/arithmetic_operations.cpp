#include "utility/arithmetic_operations.h"
#include <cassert>

size_t mod(int x, size_t n) {
    assert(n > 0);

    size_t remainder = abs(x) % n;

    if (x < 0 && remainder != 0) {
        return n - remainder;
    }

    return remainder;
}