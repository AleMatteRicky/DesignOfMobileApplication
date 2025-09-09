#pragma once

#include <cmath>

/**
 * Implementation of the modulo operator as positive remainder.
 * In contrast with the C/C++ operator '%' the remainder is always between 0 and n-1, both included.
 * @param x the dividend
 * @param n the divisor
 * @return x mod n 
 */
size_t mod(int x, size_t n);