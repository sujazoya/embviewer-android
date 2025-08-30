#ifndef PATTERN_H
#define PATTERN_H

#include <string>
#include <vector>

// A simple class to demonstrate usage
class Pattern {
public:
    Pattern(const std::string& name);

    std::string getName() const;
    void addValue(int v);
    const std::vector<int>& getValues() const;

private:
    std::string name;
    std::vector<int> values;
};

#endif // PATTERN_H
