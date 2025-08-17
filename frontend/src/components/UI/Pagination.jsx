import React from "react";

const Pagination = ({
  page = 0,
  size = 20,
  totalPages = 1,
  totalElements = 0,
  onPageChange,
}) => {
  const canPrev = page > 0;
  const canNext = page < totalPages - 1;
  const start = totalElements === 0 ? 0 : page * size + 1;
  const end = Math.min((page + 1) * size, totalElements);

  return (
    <div className="flex items-center justify-between mt-4">
      <div className="text-sm text-gray-600">
        Showing <span className="font-medium">{start}</span> to{" "}
        <span className="font-medium">{end}</span> of {totalElements} results
      </div>
      <div className="flex items-center gap-2">
        <button
          className="px-3 py-1.5 text-sm rounded-md border border-gray-300 text-gray-700 hover:bg-gray-50 disabled:opacity-50"
          disabled={!canPrev}
          onClick={() => canPrev && onPageChange(page - 1)}
        >
          Prev
        </button>
        <span className="text-sm text-gray-600">
          Page {totalPages === 0 ? 0 : page + 1} of {Math.max(totalPages, 1)}
        </span>
        <button
          className="px-3 py-1.5 text-sm rounded-md border border-gray-300 text-gray-700 hover:bg-gray-50 disabled:opacity-50"
          disabled={!canNext}
          onClick={() => canNext && onPageChange(page + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
};

export default Pagination;
