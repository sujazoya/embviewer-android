#include "pattern.h"

Pattern::Pattern(const std::string& name) : name(name) {}

std::string Pattern::getName() const {
    return name;
}

void Pattern::addValue(int v) {
    values.push_back(v);
}

const std::vector<int>& Pattern::getValues() const {
    return values;
}
