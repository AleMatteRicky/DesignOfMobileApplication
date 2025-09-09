#pragma once

#include <string>
#include <vector>

namespace model {

template <typename ItemType>
class ModelList {
public:
    virtual void add(ItemType const& item) { m_items.push_back(item); }

    /**
     * Returns the items from pos included and removes them from the this
     * ModelList
     * @param pos of the first item to return and erases
     * @return vector of items stored in this ModelList from pos
     */
    virtual std::vector<ItemType> drop(size_t pos) {
        if (m_items.empty())
            return std::vector<ItemType>();
        auto it = m_items.begin() + pos;
        std::vector<ItemType> res(it, m_items.end());
        m_items.erase(it, m_items.end());
        return res;
    }

private:
    std::vector<ItemType> m_items;
};
}  // namespace model